package me.salamander.mallet.compiler.constant;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class Constant implements Value {
    private Object value;
    private Value original;

    public Constant(Object value, Value original) {
        this.value = value;
        this.original = original;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
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
