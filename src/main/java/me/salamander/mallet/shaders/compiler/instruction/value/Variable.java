package me.salamander.mallet.shaders.compiler.instruction.value;

import it.unimi.dsi.fastutil.Hash;
import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Variable implements Location, Value{
    public static final Hash.Strategy<? super Variable> TYPED_STRATEGY = new Hash.Strategy<Variable>() {
        @Override
        public int hashCode(Variable o) {
            return Objects.hash(o.index, o.typeFirstGuess, o.variableType);
        }

        @Override
        public boolean equals(Variable o1, Variable o2) {
            if (o1 == o2) return true;
            if (o1 == null || o2 == null) return false;
            return o1.index == o2.index && o1.typeFirstGuess.equals(o2.typeFirstGuess) && o1.variableType.equals(o2.variableType);
        }
    };
    private Type typeFirstGuess;
    private int index;
    private VariableType variableType;

    public Variable(Type valueType, int index, VariableType variableType){
        this.typeFirstGuess = valueType;
        this.index = index;
        this.variableType = variableType;
    }

    public Type getTypeFirstGuess(){
        return typeFirstGuess;
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

    public void setTypeFirstGuess(Type typeFirstGuess) {
        this.typeFirstGuess = typeFirstGuess;
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
        return typeFirstGuess;
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
        return new Variable(typeFirstGuess, index, variableType);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return varMutability.get(this);
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        sb.append(ctx.varName(this));
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
