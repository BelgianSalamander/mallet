package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.value.Value;

import java.util.List;

public class IfElseASTNode extends ASTNode {
    private final List<ASTNode> ifTrue;
    private final List<ASTNode> ifFalse;
    private final Value condition;

    public IfElseASTNode(List<ASTNode> ifTrue, List<ASTNode> ifFalse, Value condition) {
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.condition = condition;
    }

    public List<ASTNode> getIfTrue() {
        return ifTrue;
    }

    public List<ASTNode> getIfFalse() {
        return ifFalse;
    }

    public Value getCondition() {
        return condition;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("if (").append(condition).append(") {\n");
        for (ASTNode node : ifTrue) {
            node.print(sb, indent + "  ");
        }
        sb.append(indent).append("} else {\n");
        for (ASTNode node : ifFalse) {
            node.print(sb, indent + "  ");
        }
        sb.append(indent).append("}\n");
    }
}
