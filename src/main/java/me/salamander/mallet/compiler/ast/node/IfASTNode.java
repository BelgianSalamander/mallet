package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.Conditions;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IfASTNode extends ASTNode{
    private final List<ASTNode> body;
    private Value condition;

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

        Value simplified = condition.trySimplify();
        if (simplified != null) {
            this.condition = simplified;
            changed = true;
        }

        if (body.size() == 1) {
            ASTNode node = body.get(0);
            if (node instanceof IfASTNode ifNode) {
                return new IfASTNode(ifNode.getBody(), Conditions.and(condition, ifNode.condition));
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

    @Override
    public ASTNode copy(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        List<ASTNode> body = this.body.stream().map(subCopier).collect(Collectors.toList());
        return new IfASTNode(body, condition);
    }

    public Value getCondition() {
        return condition;
    }
}
