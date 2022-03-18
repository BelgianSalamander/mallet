package me.salamander.mallet.shaders.glsltypes;

import me.salamander.mallet.shaders.annotation.ReturnMutable;
import org.joml.Vector3f;

public record Vec3(float x, float y, float z) {
    public Vec3(Vector3f v) {
        this(v.x, v.y, v.z);
    }

    public Vec3(float n) {
        this(n, n, n);
    }

    public Vec3 mul(float n) {
        return new Vec3(x * n, y * n, z * n);
    }

    public Vec3 mul(Vec3 v) {
        return new Vec3(x * v.x, y * v.y, z * v.z);
    }

    @ReturnMutable
    public Vector3f toVector3f() {
        return new Vector3f(x, y, z);
    }

    @Override
    public String toString() {
        return "vec3(" + x + ", " + y + ", " + z + ")";
    }
}
