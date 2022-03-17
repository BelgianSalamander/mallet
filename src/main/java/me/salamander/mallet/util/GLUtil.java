package me.salamander.mallet.util;

import me.salamander.mallet.shaders.glsltypes.Vec2;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec3i;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import org.objectweb.asm.Type;

import static org.lwjgl.opengl.GL45.*;

public class GLUtil {
    public static Type getType(int glEnum) {
        return switch (glEnum) {
            case GL_BYTE -> Type.BYTE_TYPE;
            case GL_UNSIGNED_BYTE -> Type.BYTE_TYPE;
            case GL_SHORT -> Type.SHORT_TYPE;
            case GL_UNSIGNED_SHORT -> Type.SHORT_TYPE;
            case GL_INT -> Type.INT_TYPE;
            case GL_UNSIGNED_INT -> Type.INT_TYPE;
            case GL_FLOAT -> Type.FLOAT_TYPE;
            case GL_DOUBLE -> Type.DOUBLE_TYPE;

            case GL_FLOAT_VEC2 -> Type.getType(Vec2.class);
            case GL_FLOAT_VEC3 -> Type.getType(Vec3.class);
            case GL_FLOAT_VEC4 -> Type.getType(Vec4.class);

            case GL_INT_VEC3 -> Type.getType(Vec3i.class);
            default -> throw new IllegalStateException("Unexpected value: " + glEnum);
        };
    }
}
