package me.salamander.mallet.shaders.compiler.analysis.usage;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.*;

public class PossibleValuesValue extends me.salamander.mallet.shaders.compiler.analysis.Value {
    final static PossibleValuesValue TOP = new PossibleValuesValue();

    private final Map<Variable, Set<Value>> possibleValues;

    public PossibleValuesValue() {
        this.possibleValues = new HashMap<>();
    }

    private PossibleValuesValue(Map<Variable, Set<Value>> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public PossibleValuesValue copy() {
        return new PossibleValuesValue(new HashMap<>(possibleValues));
    }

    public void set(Variable value, Value possibleValue) {
        possibleValues.put(value, makeSet(Set.of(possibleValue)));
    }

    public PossibleValuesValue merge(PossibleValuesValue other) {
        Map<Variable, Set<Value>> newValues = new HashMap<>();

        for (Map.Entry<Variable, Set<Value>> entry : possibleValues.entrySet()) {
            Variable key = entry.getKey();
            Set<Value> values = entry.getValue();
            newValues.put(key, makeSet(values));
        }

        for (Map.Entry<Variable, Set<Value>> entry : other.possibleValues.entrySet()) {
            Variable key = entry.getKey();
            Set<Value> values = entry.getValue();
            newValues.computeIfAbsent(key, k -> makeSet()).addAll(values);
        }

        return new PossibleValuesValue(newValues);
    }

    private static Set<Value> makeSet(){
        return new ObjectOpenCustomHashSet<>(new Hash.Strategy<>() {
            @Override
            public int hashCode(Value o) {
                return System.identityHashCode(o);
            }

            @Override
            public boolean equals(Value a, Value b) {
                return a == b;
            }
        });
    }

    private static Set<Value> makeSet(Collection<Value> initial){
        return new ObjectOpenCustomHashSet<>(initial, new Hash.Strategy<>() {
            @Override
            public int hashCode(Value o) {
                return System.identityHashCode(o);
            }

            @Override
            public boolean equals(Value a, Value b) {
                return a == b;
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PossibleValuesValue that = (PossibleValuesValue) o;
        return Objects.equals(possibleValues, that.possibleValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(possibleValues);
    }

    public Set<Value> get(Variable variable) {
        return possibleValues.getOrDefault(variable, Collections.emptySet());
    }
}
