package me.salamander.mallet.shaders.compiler.analysis.definition;

import me.salamander.mallet.shaders.compiler.analysis.SemiLattice;
import me.salamander.mallet.shaders.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import org.objectweb.asm.Type;

public class DefinitionSemilattice extends SemiLattice<DefinitionValue> {
    private final int[] argIndices;

    public DefinitionSemilattice(Type[] allArgTypes) {
        super(Order.FORWARDS);

        argIndices = new int[allArgTypes.length];

        int i = 0;
        for (Type type : allArgTypes) {
            argIndices[i] = i;
            i += type.getSize();
        }
    }


    @Override
    public DefinitionValue getHeadValue() {
        return new DefinitionValue(argIndices);
    }

    @Override
    public DefinitionValue getTop() {
        return new DefinitionValue();
    }

    @Override
    public DefinitionValue execute(DefinitionValue value, Instruction instruction) {
        if (instruction instanceof AssignmentInstruction assign) {
            if (assign.getLocation() instanceof Variable var) {
                return value.set(var, assign);
            }
        }

        return value;
    }

    @Override
    public DefinitionValue meet(DefinitionValue a, DefinitionValue b) {
        return a.merge(b);
    }

    @Override
    public DefinitionValue[] makeArray(int size) {
        return new DefinitionValue[size];
    }
}
