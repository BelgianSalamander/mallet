package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.annotation.NullableType;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StructType extends MalletType {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final List<Field> fields;
    private final boolean nullable;
    private int size, alignment;
    private final int[] offsets;
    private BiConsumer<ByteBuffer, Object> cachedWriter = null;

    public StructType(Type type, MalletContext ctx) {
        super(type, ctx);

        List<Field> fields = new ArrayList<>();
        Class<?> clazz = Util.getClass(type);
        Class<?> baseClass = clazz;
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            clazz = clazz.getSuperclass();
        }

        this.fields = fields;
        this.nullable = ctx.getAnnotations(baseClass).hasAnnotation(NullableType.class);
        this.offsets = new int[fields.size() + (nullable ? 1 : 0)];

        calculateLayout();
    }

    private void calculateLayout() {
        int alignment = 0;
        int size = 0;
        int currLocation = 0;

        int i = 0;

        if (nullable) {
            offsets[i] = 0;
            alignment = 1;
            size = 1;
            currLocation = 1;
            i++;
        }

        for (Field field : fields) {
            MalletType fieldType = context.getType(Type.getType(field.getType()));

            int fieldSize = fieldType.getSize();
            int fieldAlignment = fieldType.getAlignment();

            int alignmentContribution = fieldSize;

            if (fieldType instanceof BasicType basicType) {
                switch (basicType.getName()) {
                    case "vec3", "ivec3" -> alignmentContribution = 12;
                    case "dvec3" -> alignmentContribution = 24;
                    case "bvec3" -> alignmentContribution = 3;
                }
            }

            if (alignmentContribution > alignment) {
                alignment = alignmentContribution;
            }

            int offset = currLocation;
            if (offset % fieldAlignment != 0) {
                offset += fieldAlignment - (offset % fieldAlignment);
            }

            offsets[i] = offset;
            currLocation = offset + fieldSize;
            size = currLocation;

            i++;
        }

        this.alignment = alignment;
        this.size = size;

        if (this.size % alignment != 0) {
            this.size += alignment - (this.size % alignment);
        }
    }

    @Override
    public void declareType(StringBuilder sb, MalletContext context) {
        sb.append("struct ");
        sb.append(this.getName());
        sb.append(" {\n");

        if (nullable) {
            sb.append(Util.indent);
            sb.append("bool isNull;\n");
        }

        for (Field field : fields) {
            sb.append(Util.indent);
            Type type = Type.getType(field.getType());
            MalletType fieldType = context.getType(type);
            sb.append(fieldType.getName());
            sb.append(" ");
            sb.append(Util.removeSpecial(field.getName()));
            sb.append(";\n");
        }

        sb.append("};\n\n");
    }

    @Override
    public void newType(StringBuilder sb, MalletContext context) {
        sb.append(this.getName());
        sb.append("(");

        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            MalletType fieldType = context.getType(Type.getType(fields.get(i).getType()));
            fieldType.newType(sb, context);
        }

        sb.append(")");
    }

    @Override
    public void make(StringBuilder sb, Object obj, MalletContext context) {
        if (obj == null) {
            if (!nullable) {
                throw new IllegalArgumentException("Cannot make null value of non-nullable struct");
            }
            makeAny(sb, context);
            return;
        }

        sb.append(this.getName());
        sb.append("(");

        if (nullable) {
            sb.append("false");

            if (fields.size() > 0) {
                sb.append(", ");
            }
        }

        try {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                MalletType field = context.getType(Type.getType(fields.get(i).getType()));
                field.make(sb, Util.get(fields.get(i), obj), context);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        sb.append(")");
    }

    @Override
    protected void makeAny(StringBuilder sb, MalletContext context) {
        sb.append(this.getName());
        sb.append("(");

        if (nullable) {
            sb.append("true");

            if (fields.size() > 0) {
                sb.append(", ");
            }
        }

        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            MalletType field = context.getType(Type.getType(fields.get(i).getType()));
            field.makeAny(sb, context);
        }

        sb.append(")");
    }

    @Override
    protected int getSize() {
        return size;
    }

    @Override
    protected int getAlignment() {
        return alignment;
    }

    @Override
    protected void printLayout(StringBuilder sb, String indent) {
        sb.append(indent);
        sb.append(this.getName());
        sb.append(" {\n");
        int i = 0;

        if (nullable) {
            sb.append(indent);
            sb.append(Util.indent);
            sb.append("bool isNull Size: 1 Alignment: 1\n");
            i++;
        }

        for (Field field : fields) {
            sb.append(indent);
            sb.append("  Offset: ").append(offsets[i]).append("\n");
            MalletType fieldType = context.getType(Type.getType(field.getType()));
            fieldType.printLayout(sb, indent + Util.indent);
            i++;
        }

        sb.append(indent);
        sb.append("};\n\n");
    }

    @Override
    public <T> T makeWriter(Class<T> itf) {
        if (!itf.equals(BiConsumer.class)) {
            throw new IllegalArgumentException("Invalid interface: " + itf);
        }

        if (cachedWriter != null) {
            return (T) cachedWriter;
        }

        ClassNode classNode = new ClassNode();

        String name = "me/salamander/mallet/generated/MalletStructWriter_" + ID_GENERATOR.getAndIncrement();

        classNode.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        classNode.interfaces.add(itf.getName().replace('.', '/'));
        classNode.signature = "Ljava/lang/Object;Ljava/util/function/BiConsumer<Ljava/nio/ByteBuffer;" + this.getJavaType().getDescriptor() + ">;";

        //Find the interface method
        Method method;

        try {
            method = itf.getMethod("accept", Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        createDefaultConstructor(classNode);

        createGenericDelegator(classNode, method);

        //Create actual method
        createGenericDelegate(classNode, method);

        Class<?> clazz = ASMUtil.load(this.getClass().getClassLoader(), classNode)[0];

        Constructor<?> constructor = clazz.getConstructors()[0];
        try {
            cachedWriter = (BiConsumer) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return (T) cachedWriter;
    }

    private void createGenericDelegate(ClassNode classNode, Method method) {
        MethodVisitor mv = classNode.visitMethod(
                Opcodes.ACC_PUBLIC,
                method.getName(),
                "(Ljava/nio/ByteBuffer;L" + this.getJavaType().getInternalName() + ";)V",
                null,
                null
        );

        Label start = new Label();
        Label end = new Label();

        mv.visitParameter("buffer", Opcodes.ACC_FINAL);
        mv.visitParameter("value", Opcodes.ACC_FINAL);

        mv.visitLocalVariable("buffer", "Ljava/lang/Object;", null, start, end, 1);
        mv.visitLocalVariable("value", "Ljava/lang/Object;", null, start, end, 2);

        mv.visitCode();
        mv.visitLabel(start);

        //Align
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitIntInsn(Opcodes.BIPUSH, getAlignment());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "me/salamander/mallet/util/Util", "align", "(Ljava/nio/ByteBuffer;I)V", false);

        makeWriterCode(mv, (mv1) -> mv1.visitVarInsn(Opcodes.ALOAD, 1), (mv1) -> mv1.visitVarInsn(Opcodes.ALOAD, 2), 3);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(end);
    }

    private void createGenericDelegator(ClassNode classNode, Method method) {
        //Create generic delegate
        MethodVisitor mv = classNode.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                method.getName(),
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
        );

        mv.visitParameter("buffer", Opcodes.ACC_FINAL);
        mv.visitParameter("value", Opcodes.ACC_FINAL);

        Label methodStart = new Label();
        Label methodEnd = new Label();

        mv.visitLocalVariable("buffer", "Ljava/lang/Object;", null, methodStart, methodEnd, 1);
        mv.visitLocalVariable("value", "Ljava/lang/Object;", null, methodStart, methodEnd, 2);

        mv.visitCode();
        mv.visitLabel(methodStart);

        mv.visitVarInsn(Opcodes.ALOAD, 0);

        //Cast
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/nio/ByteBuffer");

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitTypeInsn(Opcodes.CHECKCAST, this.getJavaType().getInternalName());

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classNode.name, method.getName(), "(Ljava/nio/ByteBuffer;" + this.getJavaType().getDescriptor() + ")V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(methodEnd);
    }

    private static void createDefaultConstructor(ClassNode classNode) {
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
        constructorMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructorMethod.visitInsn(Opcodes.RETURN);
    }

    @Override
    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
        //Store start pos
        Label subStart = new Label();
        Label subEnd = new Label();

        mv.visitLabel(subStart);
        bufferLoader.accept(mv);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "()I", false);
        int bufferStartPosIndex = baseVarIndex++;
        mv.visitLocalVariable("bufferStartPos", "I", null, subStart, subEnd, bufferStartPosIndex);
        mv.visitVarInsn(Opcodes.ISTORE, bufferStartPosIndex);

        int currPosition = 0;

        int offsetIndex = 0;
        //Write nullable
        if (nullable) {
            /*
             * if (obj == null) {
             *   byteBuffer.putByte((byte) 1);
             *   byteBuffer.position(byteBuffer.position() + this.size - 1);
             *   return;
             * } else {
             *  byteBuffer.putByte((byte) 0);
             * }
             */

            Label isNull = new Label();
            Label end = new Label();

            objectLoader.accept(mv);
            mv.visitJumpInsn(Opcodes.IFNULL, isNull);

            bufferLoader.accept(mv);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putByte", "(B)Ljava/nio/ByteBuffer;", false);
            mv.visitJumpInsn(Opcodes.GOTO, end);

            mv.visitLabel(isNull);
            bufferLoader.accept(mv);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putByte", "(B)Ljava/nio/ByteBuffer;", false);
            bufferLoader.accept(mv);
            mv.visitVarInsn(Opcodes.ILOAD, bufferStartPosIndex);
            ASMUtil.visitIntConstant(mv, getSize());
            mv.visitInsn(Opcodes.IADD);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "(I)Ljava/nio/ByteBuffer;", false);
            mv.visitInsn(Opcodes.RETURN);

            currPosition += 1;

            offsetIndex++;
        }

        //Write fields
        int fieldVarIndex = baseVarIndex++;

        int fieldIndex = 0;
        for (Field field : fields) {
            Label fieldStart = new Label();
            Label fieldEnd = new Label();

            mv.visitLabel(fieldStart);
            Type objectType = Type.getType(field.getType());
            MalletType objectMalletType = context.getType(objectType);

            mv.visitLocalVariable("field" + (fieldIndex++), objectType.getDescriptor(), null, fieldStart, fieldEnd, fieldVarIndex);

            //Align
            int offset = offsets[offsetIndex++];

            if (offset != currPosition) {
                bufferLoader.accept(mv);
                mv.visitVarInsn(Opcodes.ILOAD, bufferStartPosIndex);
                ASMUtil.visitIntConstant(mv, offset);
                mv.visitInsn(Opcodes.IADD);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "(I)Ljava/nio/ByteBuffer;", false);
                currPosition = offset;
            }

            //Get field
            objectLoader.accept(mv);
            ASMUtil.visitField(mv, field);
            mv.visitVarInsn(objectType.getOpcode(Opcodes.ISTORE), fieldVarIndex);

            int loadOpcode = objectType.getOpcode(Opcodes.ILOAD);

            //Write field
            objectMalletType.makeWriterCode(mv, bufferLoader, mv1 -> mv1.visitVarInsn(loadOpcode, fieldVarIndex), fieldVarIndex);
            currPosition += objectMalletType.getSize();

            mv.visitLabel(fieldEnd);
        }

        mv.visitLabel(subEnd);
    }

    @Override
    public Set<Type> dependsOn() {
        Set<Type> dependsOn = new HashSet<>();

        for (Field field : fields) {
            dependsOn.add(Type.getType(field.getType()));
        }

        return dependsOn;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }
}
