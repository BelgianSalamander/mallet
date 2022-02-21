package me.salamander.mallet.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.compiler.cfg.instruction.instructions.CFGSpecialInstruction;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.Label;
import me.salamander.mallet.compiler.instruction.LabelInstruction;
import me.salamander.mallet.util.Util;

import java.io.PrintStream;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public class InstructionNode extends CFGNode {
    public Instruction instruction;

    public InstructionNode(Instruction instruction, int id, InstructionCFG parent) {
        super(id, parent);
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
        if(this.instruction instanceof CFGSpecialInstruction specialInsn) {
            specialInsn.replaceTarget(oldSuccessor, newSuccessor);
        }else if(oldSuccessor instanceof InstructionNode oldInstructionNode && newSuccessor instanceof InstructionNode newInstructionNode) {
            if(oldInstructionNode.instruction instanceof LabelInstruction labelInstruction && newInstructionNode.instruction instanceof LabelInstruction newLabelInstruction) {
                Instruction.replaceTargetLabel(
                        this.instruction,
                        labelInstruction.getLabel(),
                        newLabelInstruction.getLabel()
                );
            }
        }

        removeSuccessor(oldSuccessor);
        addSuccessor(newSuccessor);
    }

    public void removeSuccessor(CFGNode node) {
        if (!this.successors.remove(node)) {
            return;
        }

        updateReachable();

        if(node.predecessors.remove(this)) {
            node.updateDominators();
        }
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
