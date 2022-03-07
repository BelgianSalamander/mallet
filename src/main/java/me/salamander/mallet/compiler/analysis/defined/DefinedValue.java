package me.salamander.mallet.compiler.analysis.defined;

import me.salamander.mallet.compiler.analysis.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DefinedValue extends Value {
    private final Set<Variable> definedVars;

    public DefinedValue() {
        this.definedVars = new HashSet<>();
    }

    private DefinedValue(Set<Variable> definedVars) {
        this.definedVars = definedVars;
    }

    public DefinedValue with(Variable var) {
        Set<Variable> newDefinedVars = new HashSet<>(definedVars);
        newDefinedVars.add(var);
        return new DefinedValue(newDefinedVars);
    }

    public DefinedValue merge(DefinedValue other) {
        Set<Variable> newDefinedVars = new HashSet<>(definedVars);
        newDefinedVars.addAll(other.definedVars);
        return new DefinedValue(newDefinedVars);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefinedValue that = (DefinedValue) o;
        return Objects.equals(definedVars, that.definedVars);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definedVars);
    }

    public Set<Variable> getDefinedVars() {
        return definedVars;
    }
}
