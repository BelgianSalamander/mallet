package me.salamander.mallet.compiler.ast.make.cfg;

import me.salamander.mallet.compiler.ast.make.cfg.instructions.CFGSpecialInstruction;
import me.salamander.mallet.compiler.ast.make.set.SETNode;
import me.salamander.mallet.compiler.instruction.Instruction;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

public class CFGNode {
    private final Set<CFGNode> predecessors;
    private final Set<CFGNode> successors;
    private final Set<CFGNode> dominators;
    private final Set<CFGNode> reachable;
    private final Instruction instruction;
    private final int id;
    public SETNode SETParent;

    public CFGNode(Instruction instruction, int id) {
        this.instruction = instruction;
        this.id = id;

        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
        this.dominators = new HashSet<>();
        this.reachable = new HashSet<>();

        this.dominators.add(this);
    }

    public void addSuccessor(CFGNode node) {
        if(!this.successors.add(node)) return;

        this.updateReachable();

        node.addPredecessor(this);
    }

    public void addPredecessor(CFGNode node) {
        if(!this.predecessors.add(node)) return;

        this.updateDominators();

        node.addSuccessor(this);
    }

    public void replaceSuccessor(CFGNode oldNode, CFGNode newNode) {
        if(this.instruction instanceof CFGSpecialInstruction specialInstruction) {
            specialInstruction.replaceTarget(oldNode, newNode);
        }

        if(this.successors.remove(oldNode)) {
            this.forceRecalculateReachable();
            oldNode.predecessors.remove(this);
            oldNode.updateDominators();

            this.addSuccessor(newNode);
        }
        this.checkReachable();
    }

    private void forceRecalculateReachable() {
        Set<CFGNode> toRecalculate = new HashSet<>();
        Stack<CFGNode> stack = new Stack<>();

        stack.push(this);

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(!toRecalculate.add(node)) continue;

            for (CFGNode predecessor : node.predecessors) {
                stack.push(predecessor);
            }
        }

        for(CFGNode node : toRecalculate) {
            node.reachable.clear();
        }

        for(CFGNode node : toRecalculate) {
            node.updateReachable();
        }
    }

    public void updateDominators() {
        Set<CFGNode> newDominators = new HashSet<>();
        Iterator<CFGNode> it = this.predecessors.iterator();

        if(it.hasNext()) {
            newDominators.addAll(it.next().dominators);

            while (it.hasNext()) {
                newDominators.retainAll(it.next().dominators);
            }
        }

        newDominators.add(this);

        boolean equal = newDominators.equals(this.dominators);
        this.dominators.clear();
        this.dominators.addAll(newDominators);

        if(!equal) {
            for(CFGNode node : this.successors) {
                node.updateDominators();
            }
        }
    }

    public void updateReachable() {
        Set<CFGNode> newReachable = new HashSet<>();

        for(CFGNode node : this.successors) {
            newReachable.add(node);
            newReachable.addAll(node.reachable);
        }

        boolean equal = newReachable.equals(this.reachable);
        this.reachable.clear();
        this.reachable.addAll(newReachable);

        if(!equal) {
            for(CFGNode node : this.predecessors) {
                node.updateReachable();
            }
        }
    }

    public int getId() {
        return id;
    }

    public void checkReachable() {
        Set<CFGNode> calculatedReachable = new HashSet<>();

        Stack<CFGNode> stack = new Stack<>();
        for (CFGNode successor : this.successors) {
            stack.push(successor);
        }

        while (!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(!calculatedReachable.add(node)) continue;

            for(CFGNode successor : node.successors) {
                stack.push(successor);
            }
        }

        if(!calculatedReachable.equals(this.reachable)) {
            throw new RuntimeException("Reachable set is not equal");
        }
    }

    public boolean canReach(CFGNode node, CFGNode withoutPassingThrough) {
        Set<CFGNode> visited = new HashSet<>();
        Stack<CFGNode> stack = new Stack<>();

        stack.push(this);

        while(!stack.isEmpty()) {
            CFGNode current = stack.pop();

            if(current == withoutPassingThrough) {
                continue;
            }

            if(!visited.add(current)) {
                continue;
            }

            if(current == node) {
                return true;
            }

            current.getSuccessors().forEach(stack::push);
        }

        return false;
    }

    public String nodeName() {
        return "node-" + this.id;
    }

    public Set<CFGNode> getPredecessors() {
        return predecessors;
    }

    public Set<CFGNode> getSuccessors() {
        return successors;
    }

    public Set<CFGNode> getDominators() {
        return dominators;
    }

    public Set<CFGNode> getReachable() {
        return reachable;
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
