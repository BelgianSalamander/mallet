package me.salamander.mallet.compiler.type;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.glsltypes.Vec2;
import me.salamander.mallet.glsltypes.Vec3;
import me.salamander.mallet.glsltypes.Vec4;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.Set;

/**
 * Types that exist in GLSL
 */
public class BasicType extends MalletType {
    public static final BasicType INT = new BasicType(Type.INT_TYPE, "int", "", "0");
    public static final BasicType FLOAT = new BasicType(Type.FLOAT_TYPE, "float", "", "0.0f");
    public static final BasicType DOUBLE = new BasicType(Type.DOUBLE_TYPE, "double", "", "0.0");
    public static final BasicType BOOLEAN = new BasicType(Type.BOOLEAN_TYPE, "bool", "", "false");

    public static final BasicType VEC2 = new BasicType(Type.getType(Vec2.class), "vec2", "", "vec2(0.0f, 0.0f)");
    public static final BasicType VEC3 = new BasicType(Type.getType(Vec3.class), "vec3", "", "vec3(0.0f, 0.0f, 0.0f)");
    public static final BasicType VEC4 = new BasicType(Type.getType(Vec4.class), "vec4", "", "vec4(0.0f, 0.0f, 0.0f, 0.0f)");
    //Long is not supported in GLSL

    private final String postfix;
    private final String defaultValue;

    protected BasicType(Type javaType, String glslName, String postfix, String defaultValue) {
        super(javaType, glslName);
        this.postfix = postfix;
        this.defaultValue = defaultValue;
    }

    @Override
    public void declareType(StringBuilder sb, GlobalCompilationContext context) {

    }

    @Override
    public void newType(StringBuilder sb, GlobalCompilationContext context) {
        sb.append(defaultValue);
    }

    @Override
    public void make(StringBuilder sb, Object obj, GlobalCompilationContext context) {
        sb.append(obj);
        sb.append(postfix);
    }

    @Override
    public Set<Type> dependsOn() {
        return Set.of();
    }
}
