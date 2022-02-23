package me.salamander.mallet.compiler.cfg.instruction;

import me.salamander.mallet.compiler.cfg.instruction.instructions.CFGSpecialInstruction;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.LabelInstruction;

public class InstructionNode extends CFGNode {
    public Instruction instruction;

    public InstructionNode(Instruction instruction, int id, InstructionCFG parent) {
        super(id, parent);
        this.instruction = instruction;
    }

    public void addSuccessor(CFGNode successor) {
        this.successors.add(successor);
        successor.predecessors.add(this);
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

    @Override
    protected String getDescription() {
        return "Instruction (" + this.instruction + ")";
    }

    public void removeSuccessor(CFGNode node) {
        this.successors.remove(node);
        node.predecessors.remove(this);
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
