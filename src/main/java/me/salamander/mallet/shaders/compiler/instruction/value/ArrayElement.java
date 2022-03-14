package me.salamander.mallet.shaders.compiler.instruction.value;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;
import me.salamander.mallet.util.ASMUtil;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ArrayElement implements Location, Value {
    private Value array;
    private Value index;

    public ArrayElement(Value array, Value index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public Type getType() {
        return ASMUtil.getSingleType(array.getType());
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return array.isInvalidatedByChangeIn(value) || index.isInvalidatedByChangeIn(value);
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> used = new ArrayList<>(array.usedVariables());
        used.addAll(index.usedVariables());
        return used;
    }

    @Override
    public boolean allowInline() {
        return array.allowInline() && index.allowInline();
    }

    @Override
    public boolean allowDuplicateInline() {
        return array.allowDuplicateInline() && index.allowDuplicateInline();
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new ArrayElement(innerValueCopier.apply(array), innerValueCopier.apply(index));
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        array.writeGLSL(sb, ctx, shaderCompiler);
        sb.append("[");
        index.writeGLSL(sb, ctx, shaderCompiler);
        sb.append("]");
    }

    @Override
    public boolean canSet(MutabilityValue value) {
        return array.getMutability(value) != Mutability.IMMUTABLE;
    }

    public Value getArray() {
        return array;
    }

    public Value getIndex() {
        return index;
    }
}
