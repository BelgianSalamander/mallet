package me.salamander.mallet.shaders.compiler.tests;

import me.salamander.mallet.shaders.annotation.In;
import me.salamander.mallet.shaders.annotation.Out;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import me.salamander.mallet.shaders.shader.VertexShader;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ExampleVertexShader extends VertexShader {
    @In
    private static Vec3 position;

    @In
    private static Vec3 color;

    @Out
    private static Vector4f fragColor;

    //TODO: Check that main has no GLSL annotations
    public static void main(Func func){
        gl_Position = new Vec4(position, 1.0f);
        Vector3f tintedColor = color.toVector3f();
        func.run(tintedColor);
        fragColor = new Vector4f(tintedColor, 1.0f);
    }

    public static interface Func {
        void run(@Out Vector3f color);
    }
}
