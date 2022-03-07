package me.salamander.mallet.glsltypes;

import org.joml.Vector2d;
import org.joml.Vector2f;

public record Vec2(float x, float y) {
    public Vec2(Vector2f v) {
        this(v.x, v.y);
    }

    public Vector2f toVector2f() {
        return new Vector2f(x, y);
    }
}
