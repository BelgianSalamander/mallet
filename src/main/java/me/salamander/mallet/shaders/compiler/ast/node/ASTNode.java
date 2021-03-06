package me.salamander.mallet.shaders.compiler.ast.node;

import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ASTNode {
    public abstract void print(StringBuilder sb, String indent);

    public void print(StringBuilder sb) {
        print(sb, "");
    }

    public @Nullable ASTNode trySimplify() {
        return null;
    }

    public abstract void visitTree(Consumer<ASTNode> consumer);

    public abstract ASTNode visitAndReplace(Function<ASTNode, ASTNode> subCopier, Function<Instruction, Instruction> instructionCopier, Function<Value, Value> valueCopier);

    public abstract void visit(ASTVisitor visitor);
}
