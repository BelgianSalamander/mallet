package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.Instruction;

public class InstructionASTNode extends ASTNode{
    private final Instruction instruction;

    public InstructionASTNode(Instruction instruction) {
        this.instruction = instruction;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append(instruction).append(";\n");
    }
}
