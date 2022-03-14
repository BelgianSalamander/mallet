package me.salamander.mallet.compiler;

import me.salamander.mallet.annotation.Buffer;
import me.salamander.mallet.shader.ComputeShader;

public class NumberTest {
    private static final int COEFF = 5;

    public static void main(String[] args) {
        GlobalCompilationContext context = new GlobalCompilationContext();

        String shaderSource = new ShaderCompiler(context, TestShader.class).compile(COEFF);
        System.out.println(shaderSource);
    }

    private static int apply(int index, int coeff) {
        index ^= coeff;
        coeff += index - 1;
        coeff ^= index;
        index += coeff;
        return index;
    }

    public static class TestShader extends ComputeShader {
        @Buffer
        private static int[] out;

        public static void main(int coeff) {
            out[gl_GlobalInvocationID.x()] = apply(gl_GlobalInvocationID.x(), coeff);
        }
    }
}
