package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.value.Value;

import java.util.List;

public class LoopASTNode extends LabelledASTBlock {
    private final Value condition;

    public LoopASTNode(String label, List<ASTNode> body, Value condition) {
        super(label, body);
        this.condition = condition;
    }

    public Value getCondition() {
        return condition;
    }

    @Override
    protected String getHead() {
        return "while (" + condition + ")";
    }
}
