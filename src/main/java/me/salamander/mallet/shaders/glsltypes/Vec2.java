package me.salamander.mallet.shaders.glsltypes;

import org.joml.Vector2f;

public record Vec2(float x, float y) {
    public Vec2(Vector2f v) {
        this(v.x, v.y);
    }

    public Vec2 normalized() {
        float len = length();

        return new Vec2(x / len, y / len);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public Vector2f toVector2f() {
        return new Vector2f(x, y);
    }

    @Override
    public String toString() {
        return "vec2(" + x + ", " + y + ")";
    }
}
