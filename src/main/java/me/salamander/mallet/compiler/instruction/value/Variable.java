package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Variable implements Location, Value{
    private Type valueType;
    private int index;
    private VariableType variableType;

    public Variable(Type valueType, int index, VariableType variableType){
        this.valueType = valueType;
        this.index = index;
        this.variableType = variableType;
    }

    public Type getValueType(){
        return valueType;
    }

    public int getIndex(){
        return index;
    }

    public VariableType getVariableType(){
        return variableType;
    }

    public boolean isStack(){
        return variableType == VariableType.STACK;
    }

    public boolean isLocal(){
        return variableType == VariableType.LOCAL;
    }

    public boolean isTemp(){
        return variableType == VariableType.SYNTHETIC;
    }

    public void setValueType(Type valueType) {
        this.valueType = valueType;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

    @Override
    public String toString() {
        return variableType.getPrefix() + "var" + index;
    }

    @Override
    public Type getType() {
        return valueType;
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        if(value instanceof Variable variable) {
            return variable.getIndex() == index && variable.getVariableType() == variableType;
        }

        return false;
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of(this);
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
        return new Variable(valueType, index, variableType);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return varMutability.get(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return index == variable.index && variableType == variable.variableType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, variableType);
    }

    @Override
    public boolean canSet(MutabilityValue value) {
        return true;
    }
}
