package me.salamander.mallet.shaders.compiler.ast.node;

import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ContinueASTNode extends ASTNode {
    private @Nullable String label;
    private boolean needsLabel = true;

    public ContinueASTNode(@Nullable String label) {
        this.label = label;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("continue");

        if(label != null) {
            sb.append(" ").append(label);
        }

        sb.append(";\n");
    }

    @Override
    public void visitTree(Consumer<ASTNode> consumer) {
        consumer.accept(this);
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        return new ContinueASTNode(label);
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.visitContinue(this);
    }

    public boolean needsLabel() {
        return needsLabel;
    }

    public void setNeedsLabel(boolean needsLabel) {
        this.needsLabel = needsLabel;
    }
}
