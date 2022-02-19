package me.salamander.mallet.compiler.instruction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SwitchInstruction implements Instruction {
    private Value value;
    private final Int2ObjectMap<Label> branches = new Int2ObjectOpenHashMap<>();
    private @Nullable Label defaultBranch;

    public SwitchInstruction(Value value) {
        this.value = value;
    }

    private SwitchInstruction(Value value, Int2ObjectMap<Label> branches, @Nullable Label defaultBranch) {
        this.value = value;
        this.branches.putAll(branches);
        this.defaultBranch = defaultBranch;
    }

    public Value getValue() {
        return value;
    }

    public Int2ObjectMap<Label> getBranches() {
        return branches;
    }

    public Label getDefaultBranch() {
        return defaultBranch;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public void setDefaultBranch(Label defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public void addBranch(int key, Label label) {
        branches.put(key, label);
    }

    public void addBranches(int min, int max, List<Label> labels){
        int val = min;
        for (int i = 0; i < labels.size(); i++, val++) {
            addBranch(val, labels.get(i));
        }
    }

    @Override
    public List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex) {
        List<Integer> indices = new ArrayList<>();
        indices.addAll(branches.int2ObjectEntrySet().stream().map(entry -> labelIndices.get(entry.getValue())).toList());

        if (defaultBranch != null) {
            indices.add(labelIndices.get(defaultBranch));
        }

        return indices;
    }

    @Override
    public List<Variable> usedVariables() {
        return value.usedVariables();
    }

    @Override
    public Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new SwitchInstruction(valueCopier.apply(value));
    }
}
