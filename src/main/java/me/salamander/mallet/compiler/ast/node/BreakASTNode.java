package me.salamander.mallet.compiler.ast.node;

import org.jetbrains.annotations.Nullable;

public class BreakASTNode extends ASTNode {
    private @Nullable String label;

    public BreakASTNode(@Nullable String label) {
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
        sb.append(indent).append("break");

        if(label != null) {
            sb.append(" ").append(label);
        }

        sb.append(";\n");
    }
}
