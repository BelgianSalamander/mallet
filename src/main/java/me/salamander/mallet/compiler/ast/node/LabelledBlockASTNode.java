package me.salamander.mallet.compiler.ast.node;

import java.util.List;

public class LabelledBlockASTNode extends LabelledASTBlock {
    public LabelledBlockASTNode(String label, List<ASTNode> body) {
        super(label, body);
    }

    @Override
    protected String getHead() {
        return "";
    }
}
