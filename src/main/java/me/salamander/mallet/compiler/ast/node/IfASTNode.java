package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.value.Value;

import java.util.List;

public class IfASTNode extends ASTNode{
    private final List<ASTNode> body;
    private final Value condition;

    public IfASTNode(List<ASTNode> body, Value condition) {
        this.body = body;
        this.condition = condition;
    }

    public List<ASTNode> getBody() {
        return body;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("if (").append(condition.toString()).append(") {\n");
        for (ASTNode node : body) {
            node.print(sb, indent + "    ");
        }
        sb.append(indent).append("}\n");
    }
}
