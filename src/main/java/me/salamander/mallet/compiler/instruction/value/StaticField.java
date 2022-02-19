package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class StaticField implements Location, Value {
    private Type fieldOwner;
    private String fieldName;
    private Type fieldDesc;

    public StaticField(Type fieldOwner, String fieldName, Type fieldDesc) {
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
    }

    public Type getFieldOwner() {
        return fieldOwner;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Type getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldOwner(Type fieldOwner) {
        this.fieldOwner = fieldOwner;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFieldDesc(Type fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    @Override
    public Type getType() {
        return fieldDesc;
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return this.equals(value);
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
        return new StaticField(fieldOwner, fieldName, fieldDesc);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticField that = (StaticField) o;
        return Objects.equals(fieldOwner, that.fieldOwner) && Objects.equals(fieldName, that.fieldName) && Objects.equals(fieldDesc, that.fieldDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldOwner, fieldName, fieldDesc);
    }

    @Override
    public String toString() {
        return fieldOwner.getClassName() + "." + fieldName;
    }

    @Override
    public boolean canSet(MutabilityValue value) {
        return true; //THIS should only be true for shader globals
    }
}
