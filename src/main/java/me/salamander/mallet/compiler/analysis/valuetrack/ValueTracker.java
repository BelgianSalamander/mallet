package me.salamander.mallet.compiler.analysis.valuetrack;

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

import java.util.*;

public class ValueTracker extends SemiLattice<ValueTrackValue> {
    protected final ShaderCompiler shaderCompiler;

    public ValueTracker(ShaderCompiler shaderCompiler) {
        super(Order.FORWARDS);
        this.shaderCompiler = shaderCompiler;
    }

    @Override
    public ValueTrackValue getHeadValue() {
        return new ValueTrackValue();
    }

    @Override
    public ValueTrackValue getTop() {
        return ValueTrackValue.TOP;
    }

    @Override
    public ValueTrackValue execute(ValueTrackValue value, Instruction instruction) {
        ValueTrackValue newValue = value.copy();
        Set<Value> invalidated = new HashSet<>();

        if(instruction instanceof AssignmentInstruction assign){
            if(assign.getValue() instanceof MethodCallValue methodCall) {
                doMethodCall(methodCall.getMethodCall(), invalidated);
            }

            if(assign.getLocation() instanceof Variable variable) {
                if(variable.getVariableType() == VariableType.STACK) {
                    newValue.set(variable, assign.getValue());
                }
            }

            newValue.invalidate(assign.getLocation(), false);
        }else if(instruction instanceof MethodCallInstruction methodCallInstruction){
            doMethodCall(methodCallInstruction.getMethodCall(), invalidated);
        }

        for(Value v : invalidated){
            newValue.invalidate(v, true);
        }

        return newValue;
    }

    private void doMethodCall(MethodCall methodCall, Set<Value> invalidated) {
        IntList invalidatedArgs = shaderCompiler.getMutatedArgs(methodCall.getInvocation());

        for (Integer arg : invalidatedArgs) {
            if(arg == -1){
                //Invalidate global state
                invalidated.addAll(shaderCompiler.getGlobalState());
            }else{
                invalidated.add(methodCall.getArgs()[arg]);
            }
        }
    }

    @Override
    public ValueTrackValue meet(ValueTrackValue a, ValueTrackValue b) {
        return a.merge(b);
    }

    @Override
    public ValueTrackValue[] makeArray(int size) {
        return new ValueTrackValue[size];
    }
}
