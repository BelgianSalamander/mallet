package me.salamander.mallet.shaders.compiler.analysis.defined;

import me.salamander.mallet.shaders.compiler.analysis.SemiLattice;
import me.salamander.mallet.shaders.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

public class DefinedSemilattice extends SemiLattice<DefinedValue> {
    public DefinedSemilattice() {
        super(Order.FORWARDS);
    }

    @Override
    public DefinedValue getHeadValue() {
        return new DefinedValue();
    }

    @Override
    public DefinedValue getTop() {
        return new DefinedValue();
    }

    @Override
    public DefinedValue execute(DefinedValue value, Instruction instruction) {
        if (instruction instanceof AssignmentInstruction assign) {
            if (assign.getLocation() instanceof Variable var) {
                return value.with(var);
            }
        }

        return value;
    }

    @Override
    public DefinedValue meet(DefinedValue a, DefinedValue b) {
        return a.merge(b);
    }

    @Override
    public DefinedValue[] makeArray(int size) {
        return new DefinedValue[size];
    }
}
