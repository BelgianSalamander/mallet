package me.salamander.mallet.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.util.Util;

import java.io.PrintStream;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

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
    protected Set<CFGNode> dominators = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
    protected Set<CFGNode> reachable = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

    public InstructionCFG parent;

    protected CFGNode(int id, InstructionCFG parent) {
        this.id = id;
        this.parent = parent;
    }

    public abstract void addSuccessor(CFGNode successor);

    public abstract void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor);

    protected void updateDominators() {
        Set<CFGNode> newDominators = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        boolean start = true;
        for (CFGNode node : this.predecessors) {
            if (start) {
                newDominators.addAll(node.dominators);
                start = false;
            } else {
                newDominators.retainAll(node.dominators);
            }
        }

        newDominators.add(this);

        boolean changed = !newDominators.equals(this.dominators);

        this.dominators = newDominators;

        if (changed) {
            for (CFGNode node : this.successors) {
                node.updateDominators();
            }
        }
    }

    protected void updateReachable() {
        Set<CFGNode> newReachable = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        newReachable.addAll(this.successors);

        for (CFGNode node : this.successors) {
            newReachable.addAll(node.reachable);
        }

        boolean changed = !newReachable.equals(this.reachable);

        this.reachable = newReachable;

        if (changed) {
            for (CFGNode node : this.predecessors) {
                node.updateReachable();
            }
        }
    }

    public void visitDepthFirstPreorder(Consumer<CFGNode> visitor) {
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        Stack<CFGNode> stack = new Stack<>();

        stack.push(this);

        while (!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if (visited.add(node)) {
                visitor.accept(node);

                for (CFGNode successor : node.successors) {
                    stack.push(successor);
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

    public void printInfo(PrintStream out) {
        out.println("Node " + this.id + ":");
        out.println("\tPredecessors:");

        for (CFGNode node : this.predecessors) {
            out.println("\t\t" + node.id);
        }

        out.println("\tSuccessors:");

        for (CFGNode node : this.successors) {
            out.println("\t\t" + node.id);
        }

        out.println("\tDominators:");

        for (CFGNode node : this.dominators) {
            out.println("\t\t" + node.id);
        }

        out.println("\tReachable:");

        for (CFGNode node : this.reachable) {
            out.println("\t\t" + node.id);
        }

        out.println("\n");
    }
}
