package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.opengl.GL45;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
