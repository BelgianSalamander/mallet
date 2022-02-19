package me.salamander.mallet.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.util.Util;

import java.io.PrintStream;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public class InstructionNode extends CFGNode {
    private Instruction instruction;

    public InstructionNode(Instruction instruction, int id) {
        super(id);
        this.instruction = instruction;

        this.dominators.add(this);
    }

    public void addSuccessor(CFGNode successor) {
        if (!this.successors.add(successor)) {
            return;
        }

        updateReachable();

        if(successor.predecessors.add(this)) {
            successor.updateDominators();
        }
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        removeSuccessor(oldSuccessor);
        addSuccessor(newSuccessor);
    }

    private void removePredecessor(InstructionNode predecessor) {
        if (!this.predecessors.remove(predecessor)) {
            return;
        }

        updateDominators();

        predecessor.removeSuccessor(this);
    }

    private void removeSuccessor(CFGNode node) {
        if (!this.successors.remove(node)) {
            return;
        }

        updateReachable();

        if(node.predecessors.remove(this)) {
            node.updateDominators();
        }
    }
}
