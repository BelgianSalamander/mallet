package me.salamander.mallet.compiler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.salamander.mallet.util.MethodCall;
import me.salamander.mallet.util.MethodInvocation;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Objects;

public class MethodInvocationWithConstants {
    private final MethodInvocation methodInvocation;
    private final Type[] actualTypes;
    private final Int2ObjectMap<Object> paramIndexToConstant;

    public MethodInvocationWithConstants(MethodInvocation methodInvocation, Type[] actualTypes, Int2ObjectMap<Object> paramIndexToConstant) {
        this.methodInvocation = methodInvocation;
        this.paramIndexToConstant = paramIndexToConstant;
        this.actualTypes = actualTypes;
    }

    public MethodInvocation getMethodInvocation() {
        return methodInvocation;
    }

    public Int2ObjectMap<Object> getParamIndexToConstant() {
        return paramIndexToConstant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInvocationWithConstants that = (MethodInvocationWithConstants) o;
        return Objects.equals(methodInvocation, that.methodInvocation) && Arrays.equals(actualTypes, that.actualTypes) && Objects.equals(paramIndexToConstant, that.paramIndexToConstant);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(methodInvocation, paramIndexToConstant);
        result = 31 * result + Arrays.hashCode(actualTypes);
        return result;
    }

    @Override
    public String toString() {
        return "MethodInvocationWithConstants{" +
                "methodInvocation=" + methodInvocation +
                ", actualTypes=" + Arrays.toString(actualTypes) +
                ", paramIndexToConstant=" + paramIndexToConstant +
                '}';
    }

    public Type[] getActualTypes() {
        Type[] types = new Type[actualTypes.length - paramIndexToConstant.size()];

        int i = 0;
        for (int j = 0; j < actualTypes.length; j++) {
            if (!paramIndexToConstant.containsKey(j)) {
                types[i++] = actualTypes[j];
            }
        }

        return types;
    }

}
