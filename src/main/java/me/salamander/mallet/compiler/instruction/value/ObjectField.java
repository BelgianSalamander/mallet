package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class ObjectField implements Location, Value{
    private Value object;
    private Type fieldOwner;
    private String fieldName;
    private Type fieldDesc;

    public ObjectField(Value object, Type fieldOwner, String fieldName, Type fieldDesc){
        this.object = object;
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
    }

    public Value getObject() {
        return object;
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

    public void setObject(Value object) {
        this.object = object;
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
        return object.isInvalidatedByChangeIn(value);
    }

    @Override
    public List<Variable> usedVariables() {
        return object.usedVariables();
    }

    @Override
    public boolean allowInline() {
        return object.allowInline();
    }

    @Override
    public boolean allowDuplicateInline() {
        return object.allowDuplicateInline();
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new ObjectField(innerValueCopier.apply(object), fieldOwner, fieldName, fieldDesc);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public String toString() {
        return object + "." + fieldName;
    }

    @Override
    public boolean canSet(MutabilityValue value) {
        return object.getMutability(value) != Mutability.IMMUTABLE;
    }
}