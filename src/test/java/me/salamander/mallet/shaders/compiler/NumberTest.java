package me.salamander.mallet.shaders.compiler;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.annotation.Buffer;
import me.salamander.mallet.shaders.annotation.Layout;
import me.salamander.mallet.shaders.annotation.Uniform;
import me.salamander.mallet.shaders.program.ShaderProgram;
import me.salamander.mallet.shaders.shader.ComputeShader;
import me.salamander.mallet.window.Window;
import me.salamander.mallet.window.WindowOptions;
import org.joml.Vector3i;

public class NumberTest {
    private static final int COEFF = 5;

    public static void main(String[] args) {
        Window window = new Window(new WindowOptions(500, 500));

        MalletContext context = new MalletContext();

        String shaderSource = new ShaderCompiler(context, TestShader.class).compile(COEFF);

        ShaderProgram program = new ShaderProgram(ShaderProgram.COMPUTE_SHADER, shaderSource);
    }

    private static int apply(int index, int coeff, int magic) {
        index ^= coeff;
        coeff += index - 1;
        coeff ^= index;
        index += coeff;
        return index + magic;
    }

    private static float doubleNumber(float n) {
        return n * 2;
    }

    @Layout(local_size_x = 10)
    public static class TestShader extends ComputeShader {
        @Uniform
        private static Vector3i magic;

        @Buffer
        private static int[] dataOut;

        public static void main(int coeff) {
            dataOut[gl_GlobalInvocationID.x()] = apply(
                    gl_GlobalInvocationID.x(),
                    coeff,
                    (int) doubleNumber(magic.x())
            );
        }
    }
}
