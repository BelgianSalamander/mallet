package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.ast.ASTVisitor;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ReturnASTNode extends ASTNode{
    private final @Nullable Value returnValue;

    public ReturnASTNode(@Nullable Value returnValue) {
        this.returnValue = returnValue;
    }

    public @Nullable Value getReturnValue() {
        return returnValue;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("return");

        if(returnValue != null) {
            sb.append(" ").append(returnValue);
        }

        sb.append(";\n");
    }

    @Override
    public void visitTree(Consumer<ASTNode> consumer) {
        consumer.accept(this);
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        return new ReturnASTNode(returnValue == null ? null : valueCopier.apply(returnValue));
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.visitReturn(this);
    }
}
