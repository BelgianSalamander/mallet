package me.salamander.mallet.compiler.ast.node;

import java.util.List;

public abstract class LabelledASTBlock extends ASTNode {
    private final List<ASTNode> body;
    private final String label;

    public LabelledASTBlock(String label, List<ASTNode> body) {
        this.label = label;
        this.body = body;
    }

    public String getLabel() {
        return label;
    }

    public List<ASTNode> getBody() {
        return body;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append(label).append(": ");
        sb.append(getHead()).append(" {\n");

        for (ASTNode node : body) {
            node.print(sb, indent + "    ");
        }

        sb.append(indent).append("}\n");
    }

    protected abstract String getHead();
}
