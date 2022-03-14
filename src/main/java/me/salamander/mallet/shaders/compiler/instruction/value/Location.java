package me.salamander.mallet.shaders.compiler.instruction.value;

import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;

public interface Location extends Value {
    boolean canSet(MutabilityValue value);
}
