package me.salamander.mallet.compiler.analysis.livevar;

import me.salamander.mallet.compiler.analysis.SemiLattice;
import me.salamander.mallet.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Variable;

public class LiveVariables extends SemiLattice<LiveVarValue> {
    public LiveVariables() {
        super(Order.BACKWARDS);
    }

    @Override
    public LiveVarValue getHeadValue() {
        return new LiveVarValue();
    }

    @Override
    public LiveVarValue getTop() {
        return new LiveVarValue();
    }

    @Override
    public LiveVarValue execute(LiveVarValue value, Instruction instruction) {
        //Kill set
        if(instruction instanceof AssignmentInstruction assign){
            if(assign.getLocation() instanceof Variable var){
                value = value.kill(var);
            }
        }

        //Gen set
        for(Variable used: instruction.usedVariables()) {
            value = value.with(used);
        }

        return value;
    }

    @Override
    public LiveVarValue meet(LiveVarValue a, LiveVarValue b) {
        return a.merge(b);
    }

    @Override
    public LiveVarValue[] makeArray(int size) {
        return new LiveVarValue[size];
    }
}
