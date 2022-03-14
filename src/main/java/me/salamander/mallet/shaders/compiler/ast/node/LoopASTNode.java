package me.salamander.mallet.shaders.compiler.ast.node;

import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.instruction.Conditions;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class LoopASTNode extends LabelledASTBlock {
    private Value condition;

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

    @Override
    public @Nullable ASTNode trySimplify() {
        boolean changed = false;

        Value simplifiedCondition = condition.trySimplify();
        if (simplifiedCondition != null) {
            condition = simplifiedCondition;
            changed = true;
        }

        if(getBody().size() >= 1) {
            ASTNode first = getBody().get(0);

            if (first instanceof IfASTNode ifNode && ifNode.getBody().size() == 1 && ifNode.getBody().get(0) instanceof BreakASTNode breakNode && Objects.equals(breakNode.getLabel(), getLabel())) {
                this.condition = Conditions.and(this.condition, Conditions.invert(ifNode.getCondition()));
                this.getBody().remove(0);
                changed = true;
            } else if (first instanceof IfElseASTNode ifElseNode && ifElseNode.getIfTrue().size() == 1 && ifElseNode.getIfTrue().get(0) instanceof BreakASTNode breakNode && Objects.equals(breakNode.getLabel(), getLabel())) {
                this.condition = Conditions.and(this.condition, Conditions.invert(ifElseNode.getCondition()));
                this.getBody().remove(0);
                this.getBody().addAll(0, ifElseNode.getIfFalse());
                changed = true;
            }

            ASTNode last = getBody().get(getBody().size() - 1);
            if (last instanceof ContinueASTNode continueNode && Objects.equals(continueNode.getLabel(), getLabel())) {
                this.getBody().remove(getBody().size() - 1);
            }
        }

        if (super.trySimplify() != null) {
            changed = true;
        }

        if (changed) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        return new LoopASTNode(getLabel(), getBody().stream().map(subCopier).toList(), valueCopier.apply(condition));
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.enterLoop(this);

        for (ASTNode node : getBody()) {
            node.visit(visitor);
        }

        visitor.exitLoop(this);
    }
}
