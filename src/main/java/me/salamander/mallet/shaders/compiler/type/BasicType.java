package me.salamander.mallet.shaders.compiler.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.glsltypes.Vec2;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec3i;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import org.objectweb.asm.Type;

import java.util.Set;

/**
 * Types that exist in GLSL
 */
public class BasicType extends MalletType {
    private final String postfix;
    private final String defaultValue;

    protected BasicType(Type javaType, String glslName, String postfix, String defaultValue, MalletContext context) {
        super(javaType, glslName, context);
        this.postfix = postfix;
        this.defaultValue = defaultValue;
    }

    @Override
    public void declareType(StringBuilder sb, MalletContext context) {

    }

    @Override
    public void newType(StringBuilder sb, MalletContext context) {
        sb.append(defaultValue);
    }

    @Override
    public void make(StringBuilder sb, Object obj, MalletContext context) {
        sb.append(obj);
        sb.append(postfix);
    }

    @Override
    protected void makeAny(StringBuilder sb, MalletContext context) {
        newType(sb, context);
    }

    @Override
    public Set<Type> dependsOn() {
        return Set.of();
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    public static BasicType[] makeTypes(MalletContext context) {
        return new BasicType[] {
                new BasicType(Type.INT_TYPE, "int", "", "0", context),
                new BasicType(Type.FLOAT_TYPE, "float", "", "0.0f", context),
                new BasicType(Type.DOUBLE_TYPE, "double", "", "0.0", context),
                new BasicType(Type.BOOLEAN_TYPE, "bool", "", "false", context),
                new BasicType(Type.getType(Vec2.class), "vec2", "", "vec2(0.0f, 0.0f)", context),
                new BasicType(Type.getType(Vec3.class), "vec3", "", "vec3(0.0f, 0.0f, 0.0f)", context),
                new BasicType(Type.getType(Vec4.class), "vec4", "", "vec4(0.0f, 0.0f, 0.0f, 0.0f)", context),
                new BasicType(Type.getType(Vec3i.class), "ivec3", "", "ivec3(0, 0, 0)", context)
        };
    }
}
