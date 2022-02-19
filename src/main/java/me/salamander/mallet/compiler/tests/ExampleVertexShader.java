package me.salamander.mallet.compiler.tests;

import me.salamander.mallet.annotation.In;
import me.salamander.mallet.annotation.Out;
import me.salamander.mallet.annotation.Uniform;
import me.salamander.mallet.shader.VertexShader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ExampleVertexShader extends VertexShader {
    @Uniform
    private static Matrix4f u_matrix;

    @In
    private static Vector3f position;

    @In
    private static Vector3f color;

    @Out
    private static Vector4f fragPosition;

    @Out
    private static Vector4f fragColor;

    @Out
    private static Vector4f testVector;

    public static void main(float f){
        calculatePosition();
        fragColor = calculateColor(color);

        Vector4f test = copy(fragColor);

        for(int i = 0; i < 10; i++) {
            if(i % 2 == 0 || i - 1 == 4){
                test.x++;
            }
        }

        testVector = new Vector4f(2.0f, 1.0f, 0.0f, -1.0f + f);
    }

    private static Vector4f calculateColor(Vector3f color) {
        return new Vector4f(color, 1.0f);
    }

    private static void calculatePosition() {
        fragPosition = new Vector4f(position, 1.0f).mul(u_matrix);
    }
}
