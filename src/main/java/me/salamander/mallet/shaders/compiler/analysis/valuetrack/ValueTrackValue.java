package me.salamander.mallet.shaders.compiler.analysis.valuetrack;

import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ValueTrackValue extends me.salamander.mallet.shaders.compiler.analysis.Value {
    final static ValueTrackValue TOP = new ValueTrackValue();

    private final Map<Variable, Value> values;

    public ValueTrackValue() {
        this.values = new HashMap<>();
    }

    private ValueTrackValue(Map<Variable, Value> values) {
        this.values = values;
    }

    public ValueTrackValue copy() {
        if(this == TOP){
            throw new IllegalStateException("Cannot copy TOP");
        }

        return new ValueTrackValue(new HashMap<>(values));
    }

    public void set(Variable var, Value value) {
        values.put(var, value);
    }

    public void invalidate(Value value, boolean unset) {

        if(unset) {
            values.values().remove(value);
        }

        Set<Variable> invalidated = new HashSet<>();

        for (Map.Entry<Variable, Value> entry : values.entrySet()) {
            if(entry.getValue().isInvalidatedByChangeIn(value) || value.isInvalidatedByChangeIn(entry.getValue())) {
                invalidated.add(entry.getKey());
            }
        }

        for (Variable var : invalidated) {
            values.remove(var);
        }
    }

    public ValueTrackValue merge(ValueTrackValue other) {
        if(this == TOP){
            return other;
        }else if(other == TOP){
            return this;
        }

        Map<Variable, Value> merged = new HashMap<>();

        for (Map.Entry<Variable, Value> entry : values.entrySet()) {
            if(other.values.containsKey(entry.getKey()) && other.values.get(entry.getKey()).equals(entry.getValue())) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }

        return new ValueTrackValue(merged);
    }

    public Value get(Variable var) {
        return values.get(var);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ValueTrackValue val){
            return values.equals(val.values);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[ ");

        for (Map.Entry<Variable, Value> entry : values.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append(" ");
        }

        sb.append("]");

        return sb.toString();
    }
}
