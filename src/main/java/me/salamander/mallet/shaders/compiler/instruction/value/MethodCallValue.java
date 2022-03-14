package me.salamander.mallet.shaders.compiler.instruction.value;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.shaders.compiler.analysis.mutability.MutabilityValue;
import me.salamander.mallet.util.MethodCall;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodCallValue implements Value {
    private MethodCall methodCall;
    private final ShaderCompiler shaderCompiler;

    public MethodCallValue(MethodCall methodCall, ShaderCompiler shaderCompiler) {
        this.methodCall = methodCall;
        this.shaderCompiler = shaderCompiler;

        if(methodCall.getInvocation().getReturnType().getSort() == Type.VOID) {
            throw new IllegalArgumentException("Method call cannot return void");
        }
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    public void setMethodCall(MethodCall methodCall) {
        if(methodCall.getInvocation().getReturnType().getSort() == Type.VOID) {
            throw new IllegalArgumentException("Method call cannot return void");
        }

        this.methodCall = methodCall;
    }

    @Override
    public Type getType() {
        return methodCall.getInvocation().getReturnType();
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        if(shaderCompiler.getGlobalState().contains(value)) {
            return true;
        }

        for(Value argument : methodCall.getArgs()) {
            if(argument.isInvalidatedByChangeIn(value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> used = new ArrayList<>();

        for(Value argument : methodCall.getArgs()) {
            used.addAll(argument.usedVariables());
        }

        return used;
    }

    @Override
    public boolean allowInline() {
        return shaderCompiler.getMutatedArgs(methodCall.getInvocation()).size() == 0;
    }

    @Override
    public boolean allowDuplicateInline() {
        return false;
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return shaderCompiler.returnsMutable(methodCall.getInvocation()) ? Mutability.PASSIVE_MUTABLE : Mutability.IMMUTABLE;
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        shaderCompiler.callMethod(sb, methodCall);
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new MethodCallValue(methodCall.copy(innerValueCopier), shaderCompiler);
    }

    @Override
    public String toString() {
        return methodCall.toString();
    }
}
