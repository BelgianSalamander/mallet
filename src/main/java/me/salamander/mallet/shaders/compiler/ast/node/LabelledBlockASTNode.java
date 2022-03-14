package me.salamander.mallet.shaders.compiler.ast.node;

import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.instruction.Conditions;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.Ref;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class LabelledBlockASTNode extends LabelledASTBlock {
    public LabelledBlockASTNode(String label, List<ASTNode> body) {
        super(label, body);
    }

    @Override
    protected String getHead() {
        return "";
    }

    @Override
    public @Nullable ASTNode trySimplify() {
        if (getBody().get(0) instanceof IfASTNode ifBlock && ifBlock.getBody().size() == 1) {
            if (ifBlock.getBody().get(0) instanceof BreakASTNode breakNode && Objects.equals(breakNode.getLabel(), getLabel())) {
                //Check that there are no other refernces to this label
                boolean hasRef = false;
                for (ASTNode node : getBody()) {
                    if (node == getBody().get(0)) {
                        continue;
                    }

                    Ref<Boolean> booleanRef = new Ref<>(false);
                    node.visitTree(n -> {
                        if (n instanceof BreakASTNode breakNode1 && Objects.equals(breakNode1.getLabel(), getLabel())) {
                            booleanRef.value = true;
                        }
                    });

                    if (booleanRef.value) {
                        hasRef = true;
                        break;
                    }
                }

                if (!hasRef) {
                    //Simplify
                    List<ASTNode> newBody = this.getBody().subList(1, this.getBody().size());
                    return new IfASTNode(newBody, Conditions.invert(ifBlock.getCondition()));
                }
            }
        }

        boolean changed = super.trySimplify() != null;

        return changed ? this : null;
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        return new LabelledBlockASTNode(getLabel(), getBody().stream().map(subCopier).toList());
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.enterLabelledBlock(this);

        for (ASTNode node : getBody()) {
            node.visit(visitor);
        }

        visitor.exitLabelledBlock(this);
    }
}
