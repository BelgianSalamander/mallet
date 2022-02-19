package me.salamander.mallet.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ASMUtil {
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
}
