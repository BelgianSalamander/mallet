package me.salamander.mallet.util;

import me.salamander.mallet.shaders.shader.Shader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public record MethodInvocation(Type methodOwner, String methodName, Type methodDesc, MethodCallType type) {
    public Type getReturnType(){
        return methodDesc.getReturnType();
    }

    /**
     * @return The argument types (including 'this')
     */
    public Type[] getArgumentTypes(){
        Type[] base = methodDesc.getArgumentTypes();
        if(type == MethodCallType.STATIC){
            return base;
        }
        Type[] result = new Type[base.length + 1];
        result[0] = methodOwner;
        System.arraycopy(base, 0, result, 1, base.length);
        return result;
    }

    public static MethodInvocation of(MethodInsnNode methodCall) {
        return new MethodInvocation(
                Type.getObjectType(methodCall.owner),
                methodCall.name,
                Type.getMethodType(methodCall.desc),
                MethodCallType.of(methodCall.getOpcode())
        );
    }

    public boolean returnsValue() {
        return getReturnType().getSort() != Type.VOID;
    }

    public boolean isCopyMethod() {
        //Check method owner

        try {
            Class<?> clazz = Class.forName(methodOwner.getClassName());

            if(!Shader.class.isAssignableFrom(clazz)){
                return false;
            }
        }catch (ClassNotFoundException e){
            return false;
        }

        return methodName.equals("copy") && methodDesc.getDescriptor().equals("(Ljava/lang/Object;)Ljava/lang/Object;") && type == MethodCallType.STATIC;
    }

    public static MethodInvocation of(String methodOwner, MethodNode methodNode) {
        MethodCallType callType = ASMUtil.isStatic(methodNode.access) ? MethodCallType.STATIC : MethodCallType.VIRTUAL;

        return new MethodInvocation(
                Type.getObjectType(methodOwner),
                methodNode.name,
                Type.getMethodType(methodNode.desc),
                callType
        );
    }

    public enum MethodCallType{
        STATIC,
        VIRTUAL,
        SPECIAL;

        public static MethodCallType of(int opcode){
            return switch (opcode) {
                case Opcodes.INVOKESTATIC -> STATIC;
                case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE -> VIRTUAL;
                case Opcodes.INVOKESPECIAL -> SPECIAL;
                default -> throw new IllegalArgumentException("Unknown opcode: " + opcode);
            };
        }
    }
}
