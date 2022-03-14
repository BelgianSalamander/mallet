package me.salamander.mallet.shaders.compiler.analysis.livevar;

import me.salamander.mallet.shaders.compiler.analysis.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LiveVarValue extends Value {
    private final Set<Variable> liveVariables;

    public Set<Variable> getLiveVariables() {
        return liveVariables;
    }

    public LiveVarValue() {
        liveVariables = new HashSet<>();
    }

    private LiveVarValue(Set<Variable> liveVariables) {
        this.liveVariables = liveVariables;
    }

    public LiveVarValue kill(Variable variable) {
        Set<Variable> newLiveVariables = new HashSet<>(liveVariables);
        newLiveVariables.remove(variable);
        return new LiveVarValue(newLiveVariables);
    }

    public LiveVarValue with(Variable variable) {
        Set<Variable> newLiveVariables = new HashSet<>(liveVariables);
        newLiveVariables.add(variable);
        return new LiveVarValue(newLiveVariables);
    }

    public LiveVarValue merge(LiveVarValue other) {
        Set<Variable> newLiveVariables = new HashSet<>(liveVariables);
        newLiveVariables.addAll(other.liveVariables);
        return new LiveVarValue(newLiveVariables);
    }

    public boolean isLive(Variable variable){
        return liveVariables.contains(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveVarValue that = (LiveVarValue) o;
        return Objects.equals(liveVariables, that.liveVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liveVariables);
    }

    @Override
    public String toString() {
        return liveVariables.toString();
    }
}
