package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.ast.ASTVisitor;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Value;

import java.util.function.Consumer;
import java.util.function.Function;

public class InstructionASTNode extends ASTNode{
    private Instruction instruction;

    public InstructionASTNode(Instruction instruction) {
        this.instruction = instruction;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append(instruction).append(";\n");
    }

    @Override
    public void visitTree(Consumer<ASTNode> consumer) {
        consumer.accept(this);
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        return new InstructionASTNode(instructionCopier.apply(instruction));
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.visitInstruction(this);
    }
}
