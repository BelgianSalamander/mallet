package me.salamander.mallet.shaders.compiler.analysis.usage;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.shaders.compiler.analysis.SemiLattice;
import me.salamander.mallet.shaders.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import me.salamander.mallet.util.Util;

import java.util.*;

public class PossibleValuesTracker extends SemiLattice<PossibleValuesValue> {
    protected final Map<Value, Collection<Instruction>> usedBy = new IdentityHashMap<>();

    public PossibleValuesTracker() {
        super(Order.FORWARDS);
    }

    @Override
    public PossibleValuesValue getHeadValue() {
        return PossibleValuesValue.TOP;
    }

    @Override
    public PossibleValuesValue getTop() {
        return PossibleValuesValue.TOP;
    }

    @Override
    public PossibleValuesValue execute(PossibleValuesValue value, Instruction instruction) {
        PossibleValuesValue newValue = value.copy();

        //Update usedBy
        List<Variable> usedVariables = instruction.usedVariables();
        for (Variable variable : usedVariables) {
            for(Value varValue: newValue.get(variable)) {
                usedBy.computeIfAbsent(varValue, v -> new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY)).add(instruction);
            }
        }

        if(instruction instanceof AssignmentInstruction assignmentInstruction) {
            if(assignmentInstruction.getLocation() instanceof Variable var) {
                newValue.set(var, assignmentInstruction.getValue());
            }
        }

        return newValue;
    }

    public Map<Value, Collection<Instruction>> getUsedBy() {
        return usedBy;
    }

    @Override
    public PossibleValuesValue meet(PossibleValuesValue a, PossibleValuesValue b) {
        return a.merge(b);
    }

    @Override
    public PossibleValuesValue[] makeArray(int size) {
        return new PossibleValuesValue[size];
    }
}
