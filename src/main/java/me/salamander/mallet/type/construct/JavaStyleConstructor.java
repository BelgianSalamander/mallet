package me.salamander.mallet.type.construct;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

public class JavaStyleConstructor implements ObjectConstructor {
    private final Class<?> clazz;
    private final String[] fields;
    private final Type[] types;
    private final String desc;

    public JavaStyleConstructor(Class<?> clazz, String... fields) {
        this.fields = fields;
        this.clazz = clazz;

        this.types = new Type[fields.length];

        for (int i = 0; i < fields.length; i++) {
            types[i] = Type.getType(clazz);
        }

        desc = Type.getMethodDescriptor(Type.VOID_TYPE, types);
    }

    @Override
    public String[] fieldsUsed() {
        return fields;
    }

    @Override
    public void construct(MethodVisitor mv, int baseVarIndex) {
        //Store fields in vars
        int[] vars = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            vars[i] = baseVarIndex;

            baseVarIndex += this.types[i].getSize();
        }

        for (int i = vars.length - 1; i >= 0; i--) {
            mv.visitVarInsn(types[i].getOpcode(Opcodes.ISTORE), vars[i]);
        }

        //Construct object
        mv.visitTypeInsn(Opcodes.NEW, types[0].getInternalName());
        mv.visitInsn(Opcodes.DUP);

        //Recall fields
        for (int i = 0; i < vars.length; i++) {
            mv.visitVarInsn(types[i].getOpcode(Opcodes.ILOAD), vars[i]);
        }

        //Invoke constructor
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz.getName().replace('.', '/'), "<init>", desc, false);
    }

    public static JavaStyleConstructor forRecord(Class<? extends Record> clazz) {
        String[] names = Arrays.stream(clazz.getRecordComponents()).map(RecordComponent::getName).toArray(String[]::new);

        return new JavaStyleConstructor(clazz, names);
    }
}
