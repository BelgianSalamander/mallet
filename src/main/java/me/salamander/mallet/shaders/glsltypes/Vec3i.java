package me.salamander.mallet.shaders.glsltypes;

import me.salamander.mallet.shaders.annotation.ReturnMutable;
import org.joml.Vector3i;

public record Vec3i(int x, int y, int z) {
    public Vec3i(Vector3i v) {
        this(v.x(), v.y(), v.z());
    }

    @ReturnMutable
    public Vector3i toVector3i() {
        return new Vector3i(x, y, z);
    }

    @Override
    public String toString() {
        return "vec3i(" + x + ", " + y + ", " + z + ")";
    }
}
