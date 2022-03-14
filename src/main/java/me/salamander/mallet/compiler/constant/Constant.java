package me.salamander.mallet.compiler.constant;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.PrimitiveConstant;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.compiler.type.MalletType;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class Constant implements Value {
    private Object value;
    private Value original;
    private final ShaderCompiler shaderCompiler;

    public Constant(Object value, Value original, ShaderCompiler shaderCompiler) {
        this.value = value;
        this.original = original;
        this.shaderCompiler = shaderCompiler;
    }

    public Object getValue() {
        return value;
    }

    public Value getOriginal() {
        return original;
    }

    @Override
    public Type getType() {
        if (original.getType().getSort() <= Type.DOUBLE) {
            return original.getType();
        } else {
            return Type.getType(value.getClass());
        }
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Variable> usedVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean allowInline() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean allowDuplicateInline() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new Constant(value, innerValueCopier.apply(original), shaderCompiler);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        String varName = this.shaderCompiler.getConstantNames().get(toPrimitiveConstant());

        if (varName == null) {
            MalletType type = ctx.getType(getType());

            type.make(sb, value, ctx);
        } else {
            sb.append(varName);
        }
    }

    private PrimitiveConstant toPrimitiveConstant() {
        return new PrimitiveConstant(
                this.value,
                ASMUtil.isPrimitive(this.getType())
        );
    }

    @Override
    public String toString() {
        return "[Constant " + value + "]( " + original + " )";
    }

    public Value unConst() {
        return original.copyValue(v -> {
            if (v instanceof Constant) {
                return ((Constant) v).unConst();
            }

            return v;
        });
    }
}
