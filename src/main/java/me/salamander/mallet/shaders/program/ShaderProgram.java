package me.salamander.mallet.shaders.program;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.salamander.mallet.MalletContext;
import me.salamander.mallet.globject.buffer.BufferObject;
import me.salamander.mallet.shaders.glsltypes.Vec2;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec3i;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import me.salamander.mallet.shaders.program.annotation.ShaderStorageBufferBinder;
import me.salamander.mallet.shaders.program.annotation.UniformSetter;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.AnnotationList;
import me.salamander.mallet.util.GLUtil;
import me.salamander.mallet.util.Util;
import org.lwjgl.system.MemoryStack;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class ShaderProgram {
    private static final AtomicLong ID_COUNTER = new AtomicLong();

    public static final String VERTEX_SHADER = "vertex";
    public static final String FRAGMENT_SHADER = "fragment";
    public static final String GEOMETRY_SHADER = "geometry";
    public static final String TESS_CONTROL_SHADER = "tess_control";
    public static final String TESS_EVALUATION_SHADER = "tess_evaluation";
    public static final String COMPUTE_SHADER = "compute";

    private static final Object2IntMap<String> shaderNameToGLEnum = new Object2IntOpenHashMap<>();

    private final int program;
    private final boolean isCompute;

    protected ShaderProgram(int program) {
        this.program = program;

        //Check is this program is on the compute shader pipeline
        isCompute = glGetProgrami(program, GL_LINK_STATUS) == GL_TRUE && glGetProgrami(program, GL_COMPUTE_WORK_GROUP_SIZE) != 0;
    }

    public void bind() {
        glUseProgram(program);
    }

    public void dispatch(int numWorkGroupsX, int numWorkGroupsY, int numWorkGroupsZ) {
        if (!isCompute) throw new IllegalStateException("This shader program is not a compute shader program");

        glDispatchCompute(numWorkGroupsX, numWorkGroupsY, numWorkGroupsZ);
    }

    private void introspect() {
        bind();

        introspectUniforms();
    }

    private void introspectUniforms() {
        int numUniforms = glGetProgrami(program, GL_ACTIVE_UNIFORMS);

        try (MemoryStack stack = stackPush()) {
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

    public static <T extends ShaderProgram> T create(Class<T> template, MalletContext context, String... args) {
        int program = compileSources(args);

        String implName = "me/salamander/mallet/generated/ProgramImpl_" + ID_COUNTER.getAndIncrement();

        ClassNode implementationClass = new ClassNode();
        implementationClass.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, implName, null, template.getName().replace('.', '/'), null);

        createConstructor(implementationClass, program);

        //Get all SSBOs
        try (MemoryStack stack = stackPush()) {
            IntBuffer ssboCount = stack.mallocInt(1);
            glGetProgramInterfaceiv(program, GL_SHADER_STORAGE_BLOCK, GL_ACTIVE_RESOURCES, ssboCount);
            int ssbos = ssboCount.get(0);
            System.out.println("Found " + ssbos + " SSBOs");

            for (int blockIndex = 0; blockIndex < ssbos; blockIndex++) {
                IntBuffer properties = stack.mallocInt(1);
                IntBuffer length = stack.mallocInt(1);

                properties.put(GL_NUM_ACTIVE_VARIABLES);
                properties.flip();
                IntBuffer numActiveVariables = stack.mallocInt(1);
                glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, blockIndex, properties, length, numActiveVariables);

                int numActiveVariablesValue = numActiveVariables.get(0);
                System.out.println("Found " + numActiveVariablesValue + " SSBO variables");

                properties.put(GL_ACTIVE_VARIABLES);
                properties.flip();
                IntBuffer blockVars = stack.mallocInt(numActiveVariablesValue);
                glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, blockIndex, properties, length, blockVars);

                for (int i = 0; i < numActiveVariablesValue; i++) {
                    String name = glGetProgramResourceName(program, GL_SHADER_STORAGE_BLOCK, blockVars.get(i));
                    System.out.println(" - " + name);
                }
            }
        }

        for (Method method : template.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || !Modifier.isAbstract(method.getModifiers())) {
                throw new IllegalArgumentException("All methods in the template must be non-static and abstract");
            }

            AnnotationList annotations = context.getAnnotations(method);

            if (annotations.hasAnnotation(UniformSetter.class)) {
                makeUniformSetter(implementationClass, method, annotations.getAnnotation(UniformSetter.class), program);
            } else if (annotations.hasAnnotation(ShaderStorageBufferBinder.class)) {
                makeShaderStorageBufferBinder(implementationClass, method, annotations.getAnnotation(ShaderStorageBufferBinder.class), program);
            }
        }

        implementationClass.visitEnd();

        Class<?>[] clazz = ASMUtil.load(ShaderProgram.class.getClassLoader(), implementationClass);

        try {
            @SuppressWarnings("unchecked")
            T instance = (T) clazz[0].getConstructors()[0].newInstance();
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createConstructor(ClassNode implementationClass, int program) {
        MethodVisitor mv = implementationClass.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        ASMUtil.visitIntConstant(mv, program);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, implementationClass.superName, "<init>", "(I)V", false);
        mv.visitInsn(Opcodes.RETURN);
    }

    private static void makeUniformSetter(ClassNode implementationClass, Method method, UniformSetter annotation, int program) {
        String uName = annotation.value();

        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("Uniform setter methods must have exactly one parameter");
        }

        //Get location TODO: Struct/Array types
        int location = glGetUniformLocation(program, uName);

        if (location == -1) {
            throw new IllegalArgumentException("Uniform " + uName + " not found in program");
        }

        //Get type in program
        try (MemoryStack stack = stackPush()) {
            IntBuffer uniformIndices = stack.mallocInt(1);
            IntBuffer params = stack.mallocInt(1);

            uniformIndices.put(0, location);

            glGetActiveUniformsiv(program, uniformIndices, GL_UNIFORM_TYPE, params);

            Type uniformType = GLUtil.getType(params.get(0));
            Class<?> uniformClass = Util.getClass(uniformType);

            if (!uniformClass.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Uniform type " + uniformClass.getName() + " does not match method parameter type " + method.getParameterTypes()[0].getName());
            }

            MethodVisitor mv = implementationClass.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
            mv.visitCode();

            makePrimitiveUniformSetter(mv, uniformClass, (mv1) -> mv1.visitVarInsn(uniformType.getOpcode(Opcodes.ILOAD), 1), location);

            mv.visitInsn(Opcodes.RETURN);
        }
    }

    private static void makePrimitiveUniformSetter(MethodVisitor mv, Class<?> uniformClass, Consumer<MethodVisitor> objectLoader, int location) {
        ASMUtil.visitIntConstant(mv, location);
        objectLoader.accept(mv);

        if (uniformClass == int.class) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform1i", "(II)V", false);
        } else if (uniformClass == float.class) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform1f", "(IF)V", false);
        } else if (uniformClass == boolean.class) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform1i", "(II)V", false);
        } else if (uniformClass == double.class) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform1d", "(ID)V", false);
        } else if (uniformClass == Vec2.class) {
            mv.visitInsn(Opcodes.DUP);
            //Stack: [location, vec, vec]
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec2", "x", "()F", false);
            //Stack: [location, vec, x]
            mv.visitInsn(Opcodes.SWAP);
            //Stack: [location, x, vec]

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec2", "y", "()F", false);
            //Stack: [location, x, y]

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform2f", "(IFF)V", false);
        } else if (uniformClass == Vec3.class) {
            mv.visitInsn(Opcodes.DUP);
            //Stack: [location, vec, vec]
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3", "x", "()F", false);
            //Stack: [location, vec, x]
            mv.visitInsn(Opcodes.SWAP);
            //Stack: [location, x, vec]

            mv.visitInsn(Opcodes.DUP);
            //Stack: [location, x, vec, vec]
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3", "y", "()F", false);
            //Stack: [location, x, vec, y]
            mv.visitInsn(Opcodes.SWAP);
            //Stack: [location, x, y, vec]

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3", "z", "()F", false);
            //Stack: [location, x, y, z]

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform3f", "(IFFF)V", false);
        } else if (uniformClass == Vec4.class) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec4", "x", "()F", false);
            mv.visitInsn(Opcodes.SWAP);

            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec4", "y", "()F", false);
            mv.visitInsn(Opcodes.SWAP);

            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec4", "z", "()F", false);
            mv.visitInsn(Opcodes.SWAP);

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec4", "w", "()F", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform4f", "(IFFFF)V", false);
        } else if (uniformClass == Vec3i.class) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3i", "x", "()I", false);
            mv.visitInsn(Opcodes.SWAP);

            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3i", "y", "()I", false);
            mv.visitInsn(Opcodes.SWAP);

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "me/salamander/mallet/shaders/glsltypes/Vec3i", "z", "()I", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL20", "glUniform3i", "(IIII)V", false);
        } else {
            throw new RuntimeException("Unsupported uniform type: " + uniformClass.getName());
        }
    }

    private static void makeShaderStorageBufferBinder(ClassNode implementationClass, Method method, ShaderStorageBufferBinder annotation, int program) {
        if (method.getParameterCount() != 1) {
            throw new RuntimeException("ShaderStorageBufferBinder methods must have exactly one parameter");
        } else if (method.getParameterTypes()[0] != BufferObject.class) {
            throw new RuntimeException("ShaderStorageBufferBinder methods must have a parameter of type BufferObject");
        }

        //Get location
        int index = glGetProgramResourceIndex(program, GL_SHADER_STORAGE_BLOCK, annotation.value());
        if (index == -1) {
            throw new RuntimeException("Could not find shader storage block: " + annotation.value());
        }

        //Get binding
        int binding;

        try (MemoryStack stack = stackPush()) {
            IntBuffer bindingBuffer = stack.mallocInt(1);
            IntBuffer props = stack.mallocInt(1);
            IntBuffer length = stack.mallocInt(1);
            props.put(0, GL_BUFFER_BINDING);

            glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, index, props, length, bindingBuffer);

            binding = bindingBuffer.get(0);

            if (binding == -1) {
                throw new RuntimeException("Could not find binding for shader storage block: " + annotation.value());
            }
        }

        String name = method.getName();
        MethodVisitor mv = implementationClass.visitMethod(
                Opcodes.ACC_PUBLIC,
                name,
                "(" + BufferObject.class.descriptorString() + ")V",
                null,
                null
        );

        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        ASMUtil.visitIntConstant(mv, GL_SHADER_STORAGE_BUFFER);
        ASMUtil.visitIntConstant(mv, binding);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, BufferObject.class.getName().replace('.', '/'), "bindBase", "(II)V", false);

        mv.visitInsn(Opcodes.RETURN);
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
