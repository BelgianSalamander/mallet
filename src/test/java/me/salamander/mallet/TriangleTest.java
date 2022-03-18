package me.salamander.mallet;

import me.salamander.mallet.globject.buffer.BufferObject;
import me.salamander.mallet.globject.vao.VAOLayout;
import me.salamander.mallet.shaders.annotation.In;
import me.salamander.mallet.shaders.annotation.Layout;
import me.salamander.mallet.shaders.annotation.Out;
import me.salamander.mallet.shaders.annotation.Uniform;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import me.salamander.mallet.shaders.program.ShaderProgram;
import me.salamander.mallet.shaders.program.annotation.UniformSetter;
import me.salamander.mallet.shaders.shader.FragmentShader;
import me.salamander.mallet.shaders.shader.VertexShader;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.window.Window;
import me.salamander.mallet.window.WindowOptions;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Type;

public class TriangleTest {
    private static final MalletContext context = new MalletContext();
    private final int NUM_FRAMES = 60 * 10;

    @Test
    public void run() {
        Window window = new Window(new WindowOptions(800, 600));

        Vertex[] vertices = new Vertex[] {
                new Vertex(new Vec3(-0.5f, 0.0f, 0.1f), new Vec3(1.0f, 0.0f, 0.0f)),
                new Vertex(new Vec3(0.5f, 0.0f, 0.1f), new Vec3(0.0f, 1.0f, 0.0f)),
                new Vertex(new Vec3(0.0f, 1.0f, 0.1f), new Vec3(0.0f, 0.0f, 1.0f))
        };

        Program program = ShaderProgram.create(Program.class, context, TriangleVertexShader.class, TriangleFragmentShader.class);
        program.bind();

        MalletType vertex = context.getType(Type.getType(Vertex.class));
        VAOLayout layout = vertex.createLayout();

        BufferObject buffer = new BufferObject();
        buffer.setDefaultBindingPoint(GL45.GL_ARRAY_BUFFER);
        buffer.bind();
        buffer.allocate(vertex.getSize() * vertices.length, GL45.GL_STATIC_DRAW);

        buffer.writeArray(buffer.getDefaultBindingPoint(), 0, vertices, vertex);

        int vao = GL45.glGenVertexArrays();
        GL45.glBindVertexArray(vao);

        layout.declareLayout(buffer);
        layout.enable();

        int framesDrawn = 0;
        window.show();
        while (!window.shouldClose() && framesDrawn < NUM_FRAMES) {
            window.clear();

            Vec3 shift = new Vec3(1, framesDrawn / (float) NUM_FRAMES, 1);
            program.setShift(shift);

            GL45.glDrawArrays(GL45.GL_TRIANGLES, 0, vertices.length);

            window.endFrame();

            GLFW.glfwPollEvents();

            framesDrawn++;
        }

        buffer.release();
        GL45.glDeleteVertexArrays(vao);
    }

    public static record Vertex(Vec3 position, Vec3 color) {

    }

    public abstract static class Program extends ShaderProgram {
        protected Program(int program) {
            super(program);
        }

        @UniformSetter("shift")
        public abstract void setShift(Vec3 shift);
    }

    public static class TriangleVertexShader extends VertexShader {
        @Uniform
        private static final Vec3 shift = new Vec3(1.0f);

        @In
        @Layout(location = 0)
        private static final Vec3 posIn = null;

        @In
        @Layout(location = 1)
        private static final Vec3 colorIn = null;

        @Out
        private static Vec4 fragColor = null;

        public static void main() {
            gl_Position = new Vec4(posIn, 1.0f);
            fragColor = new Vec4(colorIn.mul(shift), 1.0f);
        }
    }

    public static class TriangleFragmentShader extends FragmentShader {
        @In
        private static final Vec4 fragColor = null;

        @Out
        private static Vec4 color = null;

        public static void main() {
            color = fragColor;
        }
    }
}
