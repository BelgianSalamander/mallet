package me.salamander.mallet.shaders.compiler.tests;

import me.salamander.mallet.shaders.annotation.In;
import me.salamander.mallet.shaders.annotation.Out;
import me.salamander.mallet.shaders.shader.FragmentShader;
import org.joml.Vector4f;

public class ExampleFragmentShader extends FragmentShader {
    @In
    private static Vector4f fragColor;

    @Out
    private static Vector4f outColor;

    public static void main() {
        outColor = fragColor;
    }
}
