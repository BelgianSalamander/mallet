package me.salamander.mallet.shaders.compiler.ast.node;

import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.instruction.Conditions;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IfElseASTNode extends ASTNode {
    private final List<ASTNode> ifTrue;
    private final List<ASTNode> ifFalse;
    private Value condition;

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

    @Override
    public @Nullable ASTNode trySimplify() {
        if (this.ifFalse.isEmpty()) {
            return new IfASTNode(this.ifTrue, this.condition);
        } else if (this.ifTrue.isEmpty()) {
            return new IfASTNode(this.ifFalse, Conditions.invert(this.condition));
        }

        boolean changed = false;

        for (int i = 0; i < this.ifTrue.size(); i++) {
            ASTNode node = this.ifTrue.get(i);
            ASTNode simplified = node.trySimplify();
            if (simplified != null) {
                this.ifTrue.set(i, simplified);
                changed = true;
            }
        }

        for (int i = 0; i < this.ifFalse.size(); i++) {
            ASTNode node = this.ifFalse.get(i);
            ASTNode simplified = node.trySimplify();
            if (simplified != null) {
                this.ifFalse.set(i, simplified);
                changed = true;
            }
        }

        Value simplified = this.condition.trySimplify();
        if (simplified != null) {
            this.condition = simplified;
            changed = true;
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

        for (ASTNode node : this.ifTrue) {
            node.visitTree(consumer);
        }

        for (ASTNode node : this.ifFalse) {
            node.visitTree(consumer);
        }
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        List<ASTNode> ifTrue = this.ifTrue.stream().map(subCopier).collect(Collectors.toList());
        List<ASTNode> ifFalse = this.ifFalse.stream().map(subCopier).collect(Collectors.toList());

        return new IfElseASTNode(ifTrue, ifFalse, valueCopier.apply(this.condition));
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.enterIfElseTrueBody(this);

        for (ASTNode node : this.ifTrue) {
            node.visit(visitor);
        }

        visitor.enterIfElseFalseBody(this);

        for (ASTNode node : this.ifFalse) {
            node.visit(visitor);
        }

        visitor.exitIfElse(this);
    }
}
