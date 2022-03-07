package me.salamander.mallet.compiler.analysis.mutability;

import it.unimi.dsi.fastutil.ints.IntList;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.analysis.SemiLattice;
import me.salamander.mallet.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.MethodCallInstruction;
import me.salamander.mallet.compiler.instruction.value.MethodCallValue;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.compiler.instruction.value.VariableType;
import me.salamander.mallet.util.MethodCall;
import me.salamander.mallet.util.MethodInvocation;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class MutabilitySemiLattice extends SemiLattice<MutabilityValue> {
    private final Map<Variable, Mutability> headMutability = new HashMap<>();
    private final ShaderCompiler shaderCompiler;

    public MutabilitySemiLattice(MethodInvocation method, ShaderCompiler shaderCompiler){
        super(Order.FORWARDS);
        this.shaderCompiler = shaderCompiler;

        IntList mutableParams = shaderCompiler.getMutatedArgs(method);
        Type[] paramTypes = method.getArgumentTypes();
        int varIndex = 0;

        for(int i = 0; i < paramTypes.length; i++){
            if(mutableParams.contains(i)){
                headMutability.put(new Variable(paramTypes[i], varIndex, VariableType.LOCAL), Mutability.MUTABLE);
            }

            varIndex += paramTypes[i].getSize();
        }
    }

    @Override
    public MutabilityValue getHeadValue() {
        return new MutabilityValue(headMutability);
    }

    @Override
    public MutabilityValue getTop() {
        return MutabilityValue.TOP;
    }

    @Override
    public MutabilityValue execute(MutabilityValue value, Instruction instruction) {
        if(instruction instanceof MethodCallInstruction methodCallInstruction) {
            checkMethodCall(methodCallInstruction.getMethodCall(), value);
        }else if(instruction instanceof AssignmentInstruction assignment) {
            if(!assignment.getLocation().canSet(value)){
                throw new MutatingImmutableValueException("Assignment location is immutable");
            }

            Value val = assignment.getValue();
            if(val instanceof MethodCallValue methodCallValue) {
                checkMethodCall(methodCallValue.getMethodCall(), value);
            }

            if(assignment.getLocation() instanceof Variable variable) {
                Mutability mutability = val.getMutability(value);

                if(mutability == Mutability.PASSIVE_MUTABLE){
                    if(assignment.getValue() instanceof Variable var) {
                        value = value.set(var, Mutability.IMMUTABLE);
                        value = value.set(variable, Mutability.MUTABLE);
                    }else{
                        value = value.set(variable, Mutability.PASSIVE_MUTABLE);
                    }
                }else if(mutability == Mutability.MUTABLE){
                    value = value.set(variable, val.makeImmutableOnAssign() ? Mutability.IMMUTABLE : Mutability.MUTABLE);
                }else{
                    value = value.set(variable, Mutability.IMMUTABLE);
                }
            }
        }

        return value;
    }

    private void checkMethodCall(MethodCall methodCall, MutabilityValue value) {
        IntList mutableParams = this.shaderCompiler.getMutatedArgs(methodCall.getInvocation());

        for (int i = 0; i < methodCall.getArgs().length; i++) {
            if(mutableParams.contains(i)) {
                if(methodCall.getArgs()[i].getMutability(value) == Mutability.IMMUTABLE) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Method call argument is immutable: ");
                    msg.append("Method :");
                    msg.append(methodCall.getInvocation());
                    msg.append(" Args: [ ");
                    for (int j = 0; j < methodCall.getArgs().length; j++) {
                        msg.append(methodCall.getArgs()[j]);
                        if(j != methodCall.getArgs().length - 1){
                            msg.append(", ");
                        }
                    }

                    msg.append(" ]");

                    msg.append(" Bad Arg: ").append(methodCall.getArgs()[i]);

                    throw new MutatingImmutableValueException(msg.toString());
                }
            }
        }
    }

    @Override
    public MutabilityValue meet(MutabilityValue a, MutabilityValue b) {
        return a.merge(b);
    }

    @Override
    public MutabilityValue[] makeArray(int size) {
        return new MutabilityValue[size];
    }
}
