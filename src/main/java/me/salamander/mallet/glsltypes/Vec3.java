package me.salamander.mallet.glsltypes;

import me.salamander.mallet.annotation.ReturnMutable;
import org.joml.Vector3f;

public record Vec3(float x, float y, float z) {
    public Vec3(Vector3f v) {
        this(v.x, v.y, v.z);
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
