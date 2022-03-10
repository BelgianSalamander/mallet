package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.salamander.mallet.compiler.MethodInvocationWithConstants;
import me.salamander.mallet.compiler.constant.Constant;
import me.salamander.mallet.compiler.instruction.value.LiteralValue;
import me.salamander.mallet.compiler.instruction.value.Value;
import org.objectweb.asm.Type;

import java.util.function.Function;

public class MethodCall {
    private MethodInvocation invocation;
    private Value[] args;

    public MethodCall(MethodInvocation invocation, Value[] args) {
        this.invocation = invocation;
        this.args = args;
    }

    public MethodInvocation getInvocation() {
        return invocation;
    }

    public Value[] getArgs() {
        return args;
    }

    public void setInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    public void setArgs(Value[] args) {
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int argOffset = 0;

        if(invocation.type() == MethodInvocation.MethodCallType.STATIC) {
            String ownerName = invocation.methodOwner().getClassName();
            sb.append(ownerName.substring(ownerName.lastIndexOf('.') + 1));
            sb.append(".");
            sb.append(invocation.methodName());
        }else if(invocation.type() == MethodInvocation.MethodCallType.VIRTUAL) {
            sb.append(args[0]);
            sb.append(".");
            sb.append(invocation.methodName());
            argOffset = 1;
        }else if(invocation.type() == MethodInvocation.MethodCallType.SPECIAL) {
            sb.append(invocation.methodOwner().getClassName());
            sb.append(".<special>");
            sb.append(invocation.methodName());
            argOffset = 1;
        }

        sb.append("(");

        for(int i = argOffset; i < args.length; i++) {
            sb.append(args[i]);

            if(i != args.length - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");

        return sb.toString();
    }

    public MethodCall copy(Function<Value, Value> valueCopier) {
        Value[] newArgs = new Value[args.length];

        for(int i = 0; i < args.length; i++) {
            newArgs[i] = valueCopier.apply(args[i]);
        }

        return new MethodCall(invocation, newArgs);
    }

    public MethodInvocationWithConstants toMethodInvocationWithConstants() {
        Int2ObjectMap<Object> constantParams = new Int2ObjectOpenHashMap<>();
        Type[] argTypes = new Type[args.length];

        for(int i = 0; i < args.length; i++) {
            Value arg = args[i];

            argTypes[i] = arg.getType();

            if (arg instanceof Constant constant) {
                constantParams.put(i, constant.getValue());
            } else if (arg instanceof LiteralValue literalValue) {
                constantParams.put(i, literalValue.getValue());
            }
        }

        return new MethodInvocationWithConstants(invocation, argTypes, constantParams);
    }
}
