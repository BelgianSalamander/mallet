package me.salamander.mallet.glsltypes;

import org.joml.Vector4f;

public record Vec4 (float x, float y, float z, float w) {
    public Vec4(Vector4f v) {
        this(v.x, v.y, v.z, v.w);
    }

    public Vec4(Vec3 position, float w) {
        this(position.x(), position.y(), position.z(), w);
    }

    public Vector4f toVector4f() {
        return new Vector4f(x, y, z, w);
    }

    @Override
    public String toString() {
        return "vec4(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
