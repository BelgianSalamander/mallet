package me.salamander.mallet.compiler.ast.node;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public abstract class LabelledASTBlock extends ASTNode {
    private final List<ASTNode> body;
    private final String label;
    private boolean needsLabel = true;

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

    public boolean needsLabel() {
        return needsLabel;
    }

    public void setNeedsLabel(boolean needsLabel) {
        this.needsLabel = needsLabel;
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

    @Override
    public @Nullable ASTNode trySimplify() {
        boolean changed = false;

        for (int i = 0; i < body.size(); i++) {
            ASTNode node = body.get(i);
            ASTNode simplified = node.trySimplify();
            if (simplified != null) {
                body.set(i, simplified);
                changed = true;
            }
        }

        if (changed) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public void visitTree(Consumer<ASTNode> consumer) {
        consumer.accept(this);
        for (ASTNode node : body) {
            node.visitTree(consumer);
        }
    }

    protected abstract String getHead();
}
