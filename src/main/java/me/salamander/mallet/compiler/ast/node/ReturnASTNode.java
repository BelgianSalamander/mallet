package me.salamander.mallet.compiler.ast.node;

import me.salamander.mallet.compiler.instruction.value.Value;
import org.jetbrains.annotations.Nullable;

public class ReturnASTNode extends ASTNode{
    private final @Nullable Value returnValue;

    public ReturnASTNode(@Nullable Value returnValue) {
        this.returnValue = returnValue;
    }

    public @Nullable Value getReturnValue() {
        return returnValue;
    }

    @Override
    public void print(StringBuilder sb, String indent) {
        sb.append(indent).append("return");

        if(returnValue != null) {
            sb.append(" ").append(returnValue);
        }

        sb.append(";\n");
    }
}
