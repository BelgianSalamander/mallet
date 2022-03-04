package me.salamander.mallet.compiler.ast.node;

import org.jetbrains.annotations.Nullable;

public class ContinueASTNode extends ASTNode {
    private @Nullable String label;

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
}
