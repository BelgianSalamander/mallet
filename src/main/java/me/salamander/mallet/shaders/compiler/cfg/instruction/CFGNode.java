package me.salamander.mallet.shaders.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.shaders.compiler.instruction.ReturnInstruction;
import me.salamander.mallet.util.Util;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class CFGNode {
    public final int id;

    /*
     * All of node A's predecessors must have A as a successor
     * All of node A's successors must have A as a predecessor
     * A nodes dominators is the set containing itself and the intersection of all of its predecessor's dominators
     * A nodes reachable nodes is the union of it's successor's dominators + it's sucessors
     */
    protected final Set<CFGNode> predecessors = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
    protected final Set<CFGNode> successors = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

    public InstructionCFG parent;

    protected CFGNode(int id, InstructionCFG parent) {
        this.id = id;
        this.parent = parent;
    }

    public abstract void addSuccessor(CFGNode successor);

    public abstract void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor);

    public void visitDepthFirstPreorder(Consumer<CFGNode> visitor){
        this.visitDepthFirstPreorder(visitor, (node, successor) -> {});
    }

    public void visitDepthFirstPreorder(Consumer<CFGNode> visitor, BiConsumer<CFGNode, CFGNode> edgeVisitor) {
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        Stack<CFGNode> stack = new Stack<>();

        stack.push(this);

        while (!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if (visited.add(node)) {
                visitor.accept(node);

                for (CFGNode successor : node.successors) {
                    stack.push(successor);

                    edgeVisitor.accept(node, successor);
                }
            }
        }
    }

    public void visitDepthFirstPostorder(Consumer<CFGNode> visitor) {
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        Stack<CFGNode> stack = new Stack<>();

        stack.push(this);

        while (!stack.isEmpty()) {
            CFGNode node = stack.peek();

            if (visited.add(node)) {
                for (CFGNode successor : node.successors) {
                    stack.push(successor);
                }
            } else {
                visitor.accept(node);
                stack.pop();
            }
        }
    }

    public boolean canReach(CFGNode other) {
        return canReach(other, node -> true, false);
    }

    public boolean canReach(CFGNode other, Predicate<? super CFGNode> visitCondition, boolean stayInside) {
        Stack<CFGNode> stack = new Stack<>();
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        stack.push(this);

        while (!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if (visited.add(node)) {
                if (!visitCondition.test(node)) {
                    continue;
                }

                if (node == other) {
                    return true;
                }

                if (node instanceof NodeWithInner nodeWithInner) {
                    for(InnerCFGNode innerCFG: nodeWithInner.innerCFGS()) {
                        if(innerCFG.getCFG().getHead().canReach(other, (node1) -> {
                            if(visited.contains(node1)) {
                                return false;
                            }

                            return visitCondition.test(node1);
                        }, stayInside)) {
                            return true;
                        }
                    }
                }

                for (CFGNode successor : (stayInside ? node.successors : node.getAllSuccessors())) {
                    stack.push(successor);
                }
            }
        }

        return false;
    }

    public boolean isDominatedBy(CFGNode other) {
        //This method is really inefficient. It starts by calculating all dominators of all of this nodes ancestors
        Map<CFGNode, Set<CFGNode>> dominators = new Object2ObjectOpenCustomHashMap<>(Util.IDENTITY_HASH_STRATEGY);

        Stack<CFGNode> stack = new Stack<>();
        stack.push(this);
        List<CFGNode> order = new ArrayList<>();

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();
            order.add(node);
            if(!dominators.containsKey(node)) {
                dominators.put(node, new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY));
                dominators.get(node).add(node);

                stack.addAll(node.getAllPredecessors());
            }
        }

        Collections.reverse(order);

        boolean changed = true;
        while(changed) {
            changed = false;
            for(CFGNode node: order) {
                Set<CFGNode> newDominators = Util.intersection(node.getAllPredecessors().stream().map(dominators::get).toList());
                newDominators.add(node);
                if(!newDominators.equals(dominators.get(node))) {
                    dominators.put(node, newDominators);
                    changed = true;
                }
            }
        }

        for (CFGNode cfgNode : dominators.get(this)) {
            while (cfgNode != null) {
                if(cfgNode == other) {
                    return true;
                }

                cfgNode = cfgNode.parent.parent;
            }
        }

        return false;
    }

    private Set<CFGNode> getLowestPredecessors() {
        Set<CFGNode> lowestPredecessors = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        Set<CFGNode> toReduce = this.getAllPredecessors();
        while (!toReduce.isEmpty()) {
            Set<CFGNode> newToReduce = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
            for(CFGNode node: toReduce) {
                if(node instanceof NodeWithInner nodeWithInner) {
                    for(InnerCFGNode innerCFG: nodeWithInner.innerCFGS()) {
                        for (Map.Entry<CFGNode, Set<CFGNode>> entry : innerCFG.getCFG().getExternalConnections().entrySet()) {
                            if(entry.getValue().contains(this)) {
                                newToReduce.add(entry.getKey());
                            }
                        }
                    }
                }else {
                    lowestPredecessors.add(node);
                }
            }
            toReduce = newToReduce;
        }

        return lowestPredecessors;
    }

    public int getShortestPathTo(Predicate<CFGNode> target) {
        //Dijkstra's algorithm
        Object2IntMap<CFGNode> distance = new Object2IntOpenCustomHashMap<>(Util.IDENTITY_HASH_STRATEGY);
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
        Set<CFGNode> toCheck = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        distance.put(this, 0);
        toCheck.add(this);

        while (!toCheck.isEmpty()) {
            CFGNode node = Collections.min(toCheck, Comparator.comparingInt(distance::getInt));

            toCheck.remove(node);
            visited.add(node);

            if(target.test(node)) {
                return distance.getInt(node);
            }

            for(CFGNode successor: node.getAllSuccessors()) {
                if(!visited.contains(successor)) {
                    int newDistance = distance.getInt(node) + 1;
                    if(!distance.containsKey(successor) || newDistance < distance.getInt(successor)) {
                        distance.put(successor, newDistance);
                        toCheck.add(successor);
                    }
                }
            }
        }

        throw new IllegalArgumentException("No path found");
    }

    protected Set<CFGNode> getAllPredecessors() {
        Set<CFGNode> predecessors = new ObjectOpenCustomHashSet<>(this.predecessors, Util.IDENTITY_HASH_STRATEGY);

        if(this == this.parent.getHead()) {
            if(this.parent.parent != null) {
                predecessors.addAll(this.parent.parent.getAllPredecessors());
            }
        }

        return predecessors;
    }

    protected abstract String getDescription();

    public void printInfo(PrintStream out) {
        out.println("Node " + this.id + ":");
        out.println("\t" + getDescription());
        out.println("\tSuccessors:");

        for (CFGNode node : getAllSuccessors()) {
            out.println("\t\t" + node.id + (!successors.contains(node) ? " (EXTERNAL)" : ""));
        }

        out.println("\n");
    }

    public boolean isReturn() {
        if(this instanceof InstructionNode insnNode){
            return insnNode.instruction instanceof ReturnInstruction;
        }

        return false;
    }

    public Set<CFGNode> getAllSuccessors(){
        Set<CFGNode> successors = new ObjectOpenCustomHashSet<>(this.successors, Util.IDENTITY_HASH_STRATEGY);
        successors.addAll(this.parent.getExternalConnections().getOrDefault(this, Set.of()));
        return successors;
    }

}
