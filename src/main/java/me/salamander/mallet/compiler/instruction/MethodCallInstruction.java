package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.util.MethodCall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class MethodCallInstruction implements Instruction {
    private MethodCall methodCall;

    public MethodCallInstruction(MethodCall methodCall) {
        this.methodCall = methodCall;
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    public void setMethodCall(MethodCall methodCall) {
        this.methodCall = methodCall;
    }

    @Override
    public String toString() {
        return methodCall.toString();
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> used = new ArrayList<>();

        for(Value value : methodCall.getArgs()) {
            used.addAll(value.usedVariables());
        }

        return used;
    }

    @Override
    public Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new MethodCallInstruction(methodCall.copy(valueCopier));
    }
}
