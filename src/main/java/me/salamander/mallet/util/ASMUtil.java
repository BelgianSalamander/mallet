package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.opengl.GL45;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class ASMUtil {
    private static final Int2ObjectMap<String> glEnumMap = new Int2ObjectOpenHashMap<>();

    public static Type getSingleType(Type arrayType){
        if(arrayType.getSort() != Type.ARRAY){
            throw new IllegalArgumentException("Type is not an array type");
        }

        return Type.getType(arrayType.getDescriptor().substring(1));
    }

    public static boolean isNumber(Type type){
        return Type.INT <= type.getSort() && type.getSort() <= Type.DOUBLE;
    }

    public static MethodNode findMethod(ClassNode classNode, String methodName, Type methodDesc) {
        return classNode.methods.stream()
                .filter(methodNode -> methodNode.name.equals(methodName) && methodNode.desc.equals(methodDesc.getDescriptor()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Method not found"));
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public static boolean isPrimitive(Type type){
        return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE;
    }

    public static void visitIntConstant(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public static void visitField(MethodVisitor mv, Field field) {
        int opcode = Modifier.isStatic(field.getModifiers()) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;

        mv.visitFieldInsn(opcode, field.getDeclaringClass().getName().replace('.', '/'), field.getName(), Type.getDescriptor(field.getType()));
    }

    public static ClassLoader makeClassLoader(ClassLoader parent, ClassNode... classNodes) {
        return new ClassLoader(parent) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                String slashedName = name.replace('.', '/');

                for (ClassNode classNode : classNodes) {
                    if (classNode.name.equals(slashedName)) {
                        byte[] bytes = write(classNode);

                        Path out = Path.of("run/generated-classes/" + slashedName + ".class");
                        try {
                            out.getParent().toFile().mkdirs();
                            out.toFile().delete();
                            out.toFile().createNewFile();

                            Files.write(out, bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return defineClass(name, bytes, 0, bytes.length);
                    }
                }

                return super.findClass(name);
            }
        };
    }

    public static Class<?>[] load(ClassLoader parent, ClassNode... classNodes) {
        ClassLoader classLoader = makeClassLoader(parent, classNodes);

        Class<?>[] classes = new Class<?>[classNodes.length];

        for (int i = 0; i < classNodes.length; i++) {
            try {
                Class<?> clazz = classLoader.loadClass(classNodes[i].name.replace('/', '.'));
                classes[i] = clazz;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return classes;
    }

    public static byte[] write(ClassNode classNode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classNode.accept(cw);

        return cw.toByteArray();
    }

    public static void createDefaultConstructor(ClassNode classNode) {
        //Create constructor
        MethodVisitor constructorMethod = classNode.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                "<init>",
                "()V",
                null,
                null
        );
        constructorMethod.visitCode();
        constructorMethod.visitVarInsn(Opcodes.ALOAD, 0);
        constructorMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, classNode.superName, "<init>", "()V", false);
        constructorMethod.visitInsn(Opcodes.RETURN);
    }

    static {
        Class<?> clazz = GL45.class;

        try {
            for (Field field : clazz.getFields()) {
                if (field.getType() == int.class && Modifier.isStatic(field.getModifiers())) {
                    glEnumMap.put((int) field.get(null), field.getName());
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
