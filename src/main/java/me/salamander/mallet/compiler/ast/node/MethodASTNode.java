package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.ast.ASTVisitor;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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

    @Override
    public @Nullable ASTNode trySimplify() {
        boolean changed = false;

        for(int i = 0; i < body.size(); i++){
            ASTNode node = body.get(i);
            ASTNode simplified = node.trySimplify();
            if(simplified != null){
                body.set(i, simplified);
                changed = true;
            }
        }

        if(changed){
            return this;
        } else {
            return null;
        }
    }

    @Override
    public void visitTree(Consumer<ASTNode> consumer) {
        consumer.accept(this);
        for(ASTNode node : body){
            node.visitTree(consumer);
        }
    }

    @Override
    public ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier) {
        List<ASTNode> newBody = body.stream().map(subCopier).collect(java.util.stream.Collectors.toList());
        return new MethodASTNode(newBody);
    }

    @Override
    public void visit(ASTVisitor visitor) {
        visitor.enterMethod(this);

        for (ASTNode node : body) {
            node.visit(visitor);
        }

        visitor.exitMethod(this);
    }
}
