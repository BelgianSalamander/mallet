package me.salamander.mallet.compiler.type;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.Set;

public abstract class MalletType {
    private final Type javaType;
    private final String glslName;

    protected MalletType(Type javaType) {
        this(javaType, Util.removeSpecial(javaType.getClassName()));
    }

    protected MalletType(Type javaType, String glslName) {
        this.javaType = javaType;
        this.glslName = glslName;
    }

    public abstract void declareType(StringBuilder sb, GlobalCompilationContext context);

    public abstract void newType(StringBuilder sb, GlobalCompilationContext context);

    public abstract void make(StringBuilder sb, Object obj, GlobalCompilationContext context);

    public abstract Set<Type> dependsOn();

    public String getName() {
        return glslName;
    }

    public Type getJavaType() {
        return javaType;
    }
}
