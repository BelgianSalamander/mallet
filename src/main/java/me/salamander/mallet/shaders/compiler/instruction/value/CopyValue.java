package me.salamander.mallet.shaders.compiler.instruction.value;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class CopyValue implements Value {
    private Value value;

    public CopyValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return value.getType();
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return this.value.isInvalidatedByChangeIn(value);
    }

    @Override
    public List<Variable> usedVariables() {
        return value.usedVariables();
    }

    @Override
    public boolean allowInline() {
        return value.allowInline();
    }

    @Override
    public boolean allowDuplicateInline() {
        return false;
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new CopyValue(innerValueCopier.apply(value));
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.MUTABLE;
    }

    @Override
    public boolean makeImmutableOnAssign() {
        return false;
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        value.writeGLSL(sb, ctx, shaderCompiler);
    }

    @Override
    public String toString() {
        return "copy(" + value + ")";
    }
}
