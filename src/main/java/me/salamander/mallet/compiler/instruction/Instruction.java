package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface Instruction {
    /**
     * @param labelIndices Indices of every label
     * @param currentIndex Current index of the instruction
     * @return The indices to which control flow may go to after this instruction is executed. A value of -1 indicates that control flow exits the known scope.
     * i.e a return instruction
     */
    default List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex){
        return List.of(currentIndex + 1);
    }

    List<Variable> usedVariables();

    Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier);
}
