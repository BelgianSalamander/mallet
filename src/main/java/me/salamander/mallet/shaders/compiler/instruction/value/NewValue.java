package me.salamander.mallet.shaders.compiler.instruction.value;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class NewValue implements Value {
    private Type type;

    public NewValue(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return false;
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of();
    }

    @Override
    public boolean allowInline() {
        return true;
    }

    @Override
    public boolean allowDuplicateInline() {
        return false;
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new NewValue(type);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.PASSIVE_MUTABLE;
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        ctx.getType(type).newType(sb, ctx);
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "new " + type.getClassName();
    }
}
