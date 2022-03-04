package me.salamander.mallet.compiler.ast.node;

import java.util.List;

public class MethodASTNode extends ASTNode{
    private final List<ASTNode> body;

    public MethodASTNode(List<ASTNode> body) {
        this.body = body;
    }

    public List<ASTNode> getBody() {
        return body;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("method {\n");

        for(ASTNode node : body){
            node.print(sb, indent + "    ");
        }

        sb.append(indent).append("}\n");
    }
}
