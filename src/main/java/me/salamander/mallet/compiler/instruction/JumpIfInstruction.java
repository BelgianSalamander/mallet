package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JumpIfInstruction implements Instruction {
    private Value condition;
    private Label target;

    public JumpIfInstruction(Value condition, Label target) {
        this.condition = condition;
        this.target = target;
    }

    public Value getCondition() {
        return condition;
    }

    public Label getTarget() {
        return target;
    }

    public void setCondition(Value condition) {
        this.condition = condition;
    }

    public void setTarget(Label target) {
        this.target = target;
    }

    @Override
    public List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex) {
        return List.of(labelIndices.get(target), currentIndex + 1);
    }

    @Override
    public List<Variable> usedVariables() {
        return condition.usedVariables();
    }

    @Override
    public Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new JumpIfInstruction(valueCopier.apply(condition), target);
    }

    @Override
    public String toString() {
        return "goto " + target.name() + " if " + condition;
    }
}
