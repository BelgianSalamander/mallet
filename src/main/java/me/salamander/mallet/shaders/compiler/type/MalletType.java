package me.salamander.mallet.shaders.compiler.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.Set;

public abstract class MalletType {
    private final Type javaType;
    private final String glslName;
    protected final MalletContext context;

    protected MalletType(Type javaType, MalletContext context) {
        this(javaType, Util.removeSpecial(javaType.getClassName()), context);
    }

    protected MalletType(Type javaType, String glslName, MalletContext context) {
        this.javaType = javaType;
        this.glslName = glslName;
        this.context = context;
    }

    public abstract void declareType(StringBuilder sb, MalletContext context);

    public abstract void newType(StringBuilder sb, MalletContext context);

    public abstract void make(StringBuilder sb, Object obj, MalletContext context);

    protected abstract void makeAny(StringBuilder sb, MalletContext context);

    public abstract Set<Type> dependsOn();

    public String getName() {
        return glslName;
    }

    public Type getJavaType() {
        return javaType;
    }

    public abstract boolean isPrimitive();

    public boolean isNullable() {
        return false;
    }

    public void checkNullability(StringBuilder glsl, Value value) {
        throw new UnsupportedOperationException("checkNullability not implemented for " + this.getClass().getSimpleName());
    }
}