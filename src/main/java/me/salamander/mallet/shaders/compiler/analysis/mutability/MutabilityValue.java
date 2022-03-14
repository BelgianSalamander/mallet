package me.salamander.mallet.shaders.compiler.analysis.mutability;

import me.salamander.mallet.shaders.compiler.analysis.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MutabilityValue extends Value {
    final static MutabilityValue TOP = new MutabilityValue();

    private final Map<Variable, Mutability> mutability;

    public MutabilityValue() {
        this.mutability = new HashMap<>();
    }

    MutabilityValue(Map<Variable, Mutability> mutability) {
        this.mutability = mutability;
    }

    public MutabilityValue set(Variable variable, Mutability mutability) {
        if(this == TOP){
            throw new IllegalStateException("Cannot set on top");
        }

        MutabilityValue newValue = new MutabilityValue(this.mutability);
        newValue.mutability.put(variable, mutability);
        return newValue;
    }

    public MutabilityValue merge(MutabilityValue other) {
        if(this == TOP) return other;
        if(other == TOP) return this;

        Map<Variable, Mutability> merged = new HashMap<>();

        for (Map.Entry<Variable, Mutability> entry : mutability.entrySet()) {
            merged.put(entry.getKey(), entry.getValue().merge(other.get(entry.getKey())));
        }

        return new MutabilityValue(merged);
    }

    @Override
    public String toString() {
        return mutability.toString();
    }

    public Mutability get(Variable variable) {
        return mutability.getOrDefault(variable, Mutability.IMMUTABLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutabilityValue that = (MutabilityValue) o;
        return Objects.equals(mutability, that.mutability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutability);
    }
}
