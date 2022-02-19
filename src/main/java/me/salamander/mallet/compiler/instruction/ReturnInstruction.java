package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReturnInstruction implements Instruction {
    private @Nullable Value value;

    public ReturnInstruction(@Nullable Value value) {
        this.value = value;
    }

    public @Nullable Value getValue() {
        return value;
    }

    public void setValue(@Nullable Value value) {
        this.value = value;
    }

    @Override
    public List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex) {
        return List.of(-1);
    }

    @Override
    public List<Variable> usedVariables() {
        if(value != null) {
            return value.usedVariables();
        }else{
            return List.of();
        }
    }

    @Override
    public Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new ReturnInstruction(value != null ? valueCopier.apply(value) : null);
    }

    @Override
    public String toString() {
        return "return " + (value != null ? value.toString() : "");
    }
}
