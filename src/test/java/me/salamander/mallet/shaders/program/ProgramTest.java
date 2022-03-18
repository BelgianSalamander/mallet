package me.salamander.mallet.shaders.program;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.globject.buffer.BufferObject;
import me.salamander.mallet.shaders.annotation.ShaderStorageBlock;
import me.salamander.mallet.shaders.annotation.Layout;
import me.salamander.mallet.shaders.annotation.Uniform;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.program.annotation.ShaderStorageBufferBinder;
import me.salamander.mallet.shaders.program.annotation.UniformSetter;
import me.salamander.mallet.shaders.shader.ComputeShader;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.window.Window;
import me.salamander.mallet.window.WindowOptions;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Type;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {
    private static final MalletContext context = new MalletContext();
    private static final int LOCAL_SIZE_X = 16;
    private static final int TEST_SIZE = 16;
    private static final int NUM_TESTS = 10;

    @Test
    public void multiplyTest() {
        long seed = (long) (Math.random() * Long.MAX_VALUE);
        System.out.println("Seed: " + seed);
        Random random = new Random(seed);

        Window window = new Window(new WindowOptions(100, 100)); //With GLFW we cannot have a headless context
        window.makeCurrent();

        MalletType vec3 = context.getType(Type.getType(Vec3.class));

        ShaderCompiler compiler = new ShaderCompiler(context, MultiplyTestComputeShader.class);
        String computeShaderSource = compiler.compile();

        System.out.println(computeShaderSource);

        MultiplyTestProgramTemplate program = ShaderProgram.create(MultiplyTestProgramTemplate.class, context, ShaderProgram.COMPUTE_SHADER, computeShaderSource);
        program.bind();

        int size = TEST_SIZE * LOCAL_SIZE_X;

        BufferObject bufferObject = new BufferObject();
        bufferObject.setDefaultBindingPoint(GL45.GL_SHADER_STORAGE_BUFFER);
        bufferObject.bind();
        bufferObject.allocate(size * 16, GL45.GL_DYNAMIC_READ);

        Vec3[] input = new Vec3[TEST_SIZE * LOCAL_SIZE_X];
        Vec3[] expectedOutput = new Vec3[TEST_SIZE * LOCAL_SIZE_X];

        BiConsumer<ByteBuffer, Consumer> reader = vec3.makeReader(Consumer.class, "this");

        for (int i = 0; i < NUM_TESTS; i++) {
            float multiplier = random.nextFloat() * 1000 - 500;
            program.setMultiplier(multiplier);

            for (int j = 0; j < input.length; j++) {
                input[j] = new Vec3(random.nextFloat(), random.nextFloat(), random.nextFloat());
                expectedOutput[j] = input[j].mul(multiplier);
            }

            bufferObject.writeArray(0, input, context);
            program.bindData(bufferObject);

            program.dispatch(TEST_SIZE, 1, 1);

            ByteBuffer generated = bufferObject.read(0, size * 16);

            for (int j = 0; j < expectedOutput.length; j++) {
                int finalJ = j;

                reader.accept(generated, (vec) -> {
                    assertEquals(expectedOutput[finalJ], vec);
                });
            }

            MemoryUtil.memFree(generated);
        }

        bufferObject.release();
    }

    @Layout(local_size_x = LOCAL_SIZE_X)
    public static abstract class MultiplyTestComputeShader extends ComputeShader {
        @Uniform
        private static float u_multiplier = 0.0f;

        @ShaderStorageBlock
        private static final Vec3[] data = new Vec3[0];

        public static void main() {
            int index = gl_GlobalInvocationID.x();

            data[index] = data[index].mul(u_multiplier);
        }
    }

    public static abstract class MultiplyTestProgramTemplate extends ShaderProgram {
        protected MultiplyTestProgramTemplate(int program) {
            super(program);
        }

        @UniformSetter("u_multiplier")
        public abstract void setMultiplier(float multiplier);

        @ShaderStorageBufferBinder("data")
        public abstract void bindData(BufferObject data);
    }
}
