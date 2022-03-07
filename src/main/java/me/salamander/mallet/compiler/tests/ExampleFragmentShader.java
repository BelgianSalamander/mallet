package me.salamander.mallet.compiler.tests;

import me.salamander.mallet.annotation.In;
import me.salamander.mallet.annotation.Out;
import me.salamander.mallet.shader.FragmentShader;
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
