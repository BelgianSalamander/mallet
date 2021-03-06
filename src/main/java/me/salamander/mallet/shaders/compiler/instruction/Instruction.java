package me.salamander.mallet.shaders.compiler.instruction;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

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

    Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier);

    static void replaceTargetLabel(Instruction instruction, Label oldLabel, Label newLabel){
        if(instruction instanceof GotoInstruction goto_){
            if(goto_.getTarget().equals(oldLabel)){
                goto_.setTarget(newLabel);
            }
        }else if(instruction instanceof JumpIfInstruction jumpIf){
            if(jumpIf.getTarget().equals(oldLabel)){
                jumpIf.setTarget(newLabel);
            }
        }else if(instruction instanceof SwitchInstruction switch_){
            if (switch_.getDefaultBranch() == oldLabel) {
                switch_.setDefaultBranch(newLabel);
            }

            IntSet change = switch_.getBranches().int2ObjectEntrySet().stream().filter(entry -> entry.getValue() == oldLabel).mapToInt(Map.Entry::getKey).collect(IntOpenHashSet::new, IntSet::add, IntSet::addAll);

            for (int i : change) {
                switch_.getBranches().put(i, newLabel);
            }
        }
    }

    void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler);
}
