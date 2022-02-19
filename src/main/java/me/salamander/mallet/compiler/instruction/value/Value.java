package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public interface Value {
    Type getType();

    boolean isInvalidatedByChangeIn(Value value);

    List<Variable> usedVariables();

    boolean allowInline(); //The only thing that cannot be inlined is a function call that mutates something
    boolean allowDuplicateInline();

    Value copyValue(Function<Value, Value> innerValueCopier);

    Mutability getMutability(MutabilityValue varMutability);

    default boolean makeImmutableOnAssign() {
        return true;
    }
}
