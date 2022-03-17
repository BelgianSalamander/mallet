package me.salamander.mallet.type.construct;

import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.type.StructType;
import me.salamander.mallet.util.Util;
import org.joml.Vector3f;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class UnsafeConstructor implements ObjectConstructor {
    private final Type type;
    private final Field[] fields;
    private final long[] fieldOffsets;

    public UnsafeConstructor(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();

        this.fields = Arrays.stream(fields).filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
        this.type = Type.getType(clazz);

        fieldOffsets = new long[this.fields.length];

        for (int i = 0; i < this.fields.length; i++) {
            fieldOffsets[i] = Util.UNSAFE.objectFieldOffset(this.fields[i]);
        }
    }

    @Override
    public String[] fieldsUsed() {
        return Arrays.stream(fields).map(Field::getName).toArray(String[]::new);
    }

    @Override
    public void construct(MethodVisitor mv, int baseVarIndex) {
        //Store fields in vars
        int[] vars = new int[fields.length];
        Type[] types = new Type[fields.length];
        for (int i = 0; i < fields.length; i++) {
            vars[i] = baseVarIndex;
            Class<?> clazz = fields[i].getType();
            Type type = types[i] = Type.getType(clazz);

            baseVarIndex += type.getSize();
        }

        for (int i = vars.length - 1; i >= 0; i--) {
            mv.visitVarInsn(types[i].getOpcode(Opcodes.ISTORE), vars[i]);
        }

        mv.visitFieldInsn(Opcodes.GETSTATIC, Util.class.getName().replace('.', '/'), "UNSAFE", "Lsun/misc/Unsafe;");
        mv.visitLdcInsn(this.type);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, this.type.getInternalName());

        int objectVarIndex = baseVarIndex++;
        mv.visitVarInsn(Opcodes.ASTORE, objectVarIndex);

        for (int i = 0; i < fields.length; i++) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, Util.class.getName().replace('.', '/'), "UNSAFE", "Lsun/misc/Unsafe;");
            mv.visitVarInsn(Opcodes.ALOAD, objectVarIndex);
            mv.visitLdcInsn(fieldOffsets[i]);
            mv.visitVarInsn(types[i].getOpcode(Opcodes.ILOAD), vars[i]);

            String methodName, methodDesc;
            if (types[i] == Type.BOOLEAN_TYPE) {
                methodName = "putBoolean";
                methodDesc = "(Ljava/lang/Object;JZ)V";
            } else if (types[i] == Type.BYTE_TYPE) {
                methodName = "putByte";
                methodDesc = "(Ljava/lang/Object;JB)V";
            } else if (types[i] == Type.CHAR_TYPE) {
                methodName = "putChar";
                methodDesc = "(Ljava/lang/Object;JC)V";
            } else if (types[i] == Type.DOUBLE_TYPE) {
                methodName = "putDouble";
                methodDesc = "(Ljava/lang/Object;JD)V";
            } else if (types[i] == Type.FLOAT_TYPE) {
                methodName = "putFloat";
                methodDesc = "(Ljava/lang/Object;JF)V";
            } else if (types[i] == Type.INT_TYPE) {
                methodName = "putInt";
                methodDesc = "(Ljava/lang/Object;JI)V";
            } else if (types[i] == Type.LONG_TYPE) {
                methodName = "putLong";
                methodDesc = "(Ljava/lang/Object;JJ)V";
            } else if (types[i] == Type.SHORT_TYPE) {
                methodName = "putShort";
                methodDesc = "(Ljava/lang/Object;JS)V";
            } else {
                methodName = "putObject";
                methodDesc = "(Ljava/lang/Object;JLjava/lang/Object;)V";
            }

            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", methodName, methodDesc, false);
        }

        mv.visitVarInsn(Opcodes.ALOAD, objectVarIndex);
    }
}
