package me.salamander.mallet.shaders.program;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL45.*;

public class ShaderProgram {
    public static final String VERTEX_SHADER = "vertex";
    public static final String FRAGMENT_SHADER = "fragment";
    public static final String GEOMETRY_SHADER = "geometry";
    public static final String TESS_CONTROL_SHADER = "tess_control";
    public static final String TESS_EVALUATION_SHADER = "tess_evaluation";
    public static final String COMPUTE_SHADER = "compute";

    private static final Object2IntMap<String> shaderNameToGLEnum = new Object2IntOpenHashMap<>();

    private final int program;

    public ShaderProgram(String... sources) {
        this(compileSources(sources));

        introspect();
    }

    protected ShaderProgram(int program) {
        this.program = program;
    }

    public void bind() {
        glUseProgram(program);
    }

    private void introspect() {
        bind();

        introspectUniforms();
    }

    private void introspectUniforms() {
        int numUniforms = glGetProgrami(program, GL_ACTIVE_UNIFORMS);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer b_size = stack.mallocInt(1);
            IntBuffer b_type = stack.mallocInt(1);

            for (int i = 0; i < numUniforms; i++) {
                String name = glGetActiveUniform(program, i, b_size, b_type);
                int location = glGetUniformLocation(program, name);
                System.out.println(name + ": " + location);
            }
        }
    }

    private static int compileSources(String... sources) {
        if ((sources.length & 1) != 0) {
            throw new IllegalArgumentException("Sources must be in pairs of (shader type, source)");
        }

        int program = glCreateProgram();
        boolean success = true;

        int[] shaders = new int[sources.length / 2];

        int i;
        for (i = 0; i < sources.length; i += 2) {
            int shaderType = shaderNameToGLEnum.getInt(sources[i]);
            String shaderSource = sources[i + 1];

            int shader = glCreateShader(shaderType);
            glShaderSource(shader, shaderSource);
            glCompileShader(shader);

            int status = glGetShaderi(shader, GL_COMPILE_STATUS);
            if (status == GL_FALSE) {
                success = false;
                System.err.println("Failed to compile shader: " + sources[i]);
                System.err.println(glGetShaderInfoLog(shader));
                System.err.println("Source: \n");

                String[] lines = shaderSource.split("\n");
                int numLines = lines.length;
                int numLength = Math.max(Integer.toString(numLines).length(), 4);
                for (int j = 0; j < numLines; j++) {
                    String line = lines[j];
                    System.err.printf("%0" + numLength + "d: %s\n", j + 1, line);
                }

                break;
            }

            shaders[i / 2] = shader;
        }

        if (!success) {
            glDeleteProgram(program);
            for (int shader : shaders) {
                glDeleteShader(shader);
            }
            throw new IllegalArgumentException("Failed to compile shader");
        }

        for (int shader : shaders) {
            glAttachShader(program, shader);
        }

        glLinkProgram(program);

        for (int shader : shaders) {
            glDeleteShader(shader);
        }

        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            System.err.println("Failed to link shader program");
            System.err.println(glGetProgramInfoLog(program));
            glDeleteProgram(program);
            throw new IllegalArgumentException("Failed to link shader program");
        }

        return program;
    }

    static {
        shaderNameToGLEnum.put(VERTEX_SHADER, GL_VERTEX_SHADER);
        shaderNameToGLEnum.put(FRAGMENT_SHADER, GL_FRAGMENT_SHADER);
        shaderNameToGLEnum.put(GEOMETRY_SHADER, GL_GEOMETRY_SHADER);
        shaderNameToGLEnum.put(TESS_CONTROL_SHADER, GL_TESS_CONTROL_SHADER);
        shaderNameToGLEnum.put(TESS_EVALUATION_SHADER, GL_TESS_EVALUATION_SHADER);
        shaderNameToGLEnum.put(COMPUTE_SHADER, GL_COMPUTE_SHADER);
    }
}
