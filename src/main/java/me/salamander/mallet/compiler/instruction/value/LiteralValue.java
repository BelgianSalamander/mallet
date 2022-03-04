package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class LiteralValue implements Value {
    private Type type;
    private Object value;

    public LiteralValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return false;
    }

    public Object getValue() {
        return value;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
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
        return true;
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new LiteralValue(type, value);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public String toString() {
        if(type == Type.INT_TYPE) {
            return value.toString();
        }else if(type == Type.FLOAT_TYPE) {
            return value.toString() + "f";
        }else if(type == Type.LONG_TYPE) {
            return value.toString() + "l";
        }else if(type == Type.DOUBLE_TYPE) {
            return value.toString() + "d";
        }else if(value instanceof String){
            return "\"" + value + "\"";
        }else if(value instanceof Boolean) {
            return value.toString();
        } else{
            throw new RuntimeException("Unknown literal type: " + type);
        }
    }
}
