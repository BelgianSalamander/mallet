package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NewArray implements Value {
    private Type elementType;
    private Value[] size;

    public NewArray(Type type, Value... size) {
        this.elementType = type;
        this.size = size;
    }

    public Type getElementType() {
        return elementType;
    }

    public Value[] getSize() {
        return size;
    }

    public void setElementType(Type elementType) {
        this.elementType = elementType;
    }

    public void setSize(Value[] size) {
        this.size = size;
    }

    @Override
    public Type getType() {
        return Type.getType("[".repeat(size.length) + elementType.getDescriptor());
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        for(Value v : size) {
            if(v.isInvalidatedByChangeIn(value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> variables = new ArrayList<>();

        for(Value v : size) {
            variables.addAll(v.usedVariables());
        }

        return variables;
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
        Value[] newSize = new Value[size.length];

        for(int i = 0; i < size.length; i++) {
            newSize[i] = innerValueCopier.apply(size[i]);
        }

        return new NewArray(elementType, newSize);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.MUTABLE;
    }
}
