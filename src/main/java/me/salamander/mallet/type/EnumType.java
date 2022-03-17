package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.ObjectField;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnumType extends MalletType{
    private final List<Field> fields = new ArrayList<>();
    private final Class<?> clazz;
    private final Object[] values;

    public EnumType(Type type, Object[] values, MalletContext context) {
        super(type, context);

        Class<?> clazz = Util.getClass(type);
        this.clazz = (Class<? extends Enum<?>>) clazz;
        this.values = values;

        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    if (clazz == Enum.class) {
                        continue;
                    }

                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public void declareType(StringBuilder sb, MalletContext context) {
        sb.append("struct ");
        sb.append(this.getName());
        sb.append(" {\n");

        sb.append(Util.indent);
        sb.append("uint ordinal;\n};\n\n");

        //Define fields
        for (Field field : fields) {
            MalletType fieldType = context.getType(Type.getType(field.getType()));
            sb.append(fieldType.getName());
            sb.append(" ");
            sb.append(this.getArrayName(field));
            sb.append(" = {\n");

            for (int i = 0; i < values.length; i++) {
                Object constant = values[i];

                if (i > 0) {
                    sb.append(",\n");
                }

                sb.append(Util.indent);
                try {
                    Object value = field.get(constant);
                    fieldType.make(sb, value, context);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }

            sb.append("\n};\n\n");
        }
    }

    private String getArrayName(Field field) {
        return this.getName() + "_" + field.getName();
    }

    private String getArrayName(ObjectField field) {
        return this.getName() + "_" + field.getFieldName();
    }

    @Override
    public void newType(StringBuilder sb, MalletContext context) {
        throw new UnsupportedOperationException("Cannot create new enum");
    }

    @Override
    protected void makeAny(StringBuilder sb, MalletContext context) {
        sb.append(this.getName()).append("(-1)");
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public int getAlignment() {
        return 4;
    }

    @Override
    protected void printLayout(StringBuilder sb, String indent) {
        sb.append(indent);
        sb.append(getName());
        sb.append("Size: ").append(getSize());
        sb.append(" Alignment: ").append(getAlignment());
    }

    @Override
    @SuppressWarnings("unchecked")
    public  <T> T makeWriter(Class<T> itf) {
        return (T) (BiConsumer<ByteBuffer, Enum<?>>) (buffer, value) -> {
            Util.align(buffer, getAlignment());
            buffer.putInt(value == null ? -1 : value.ordinal());
        };
    }

    @Override
    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
        bufferLoader.accept(mv);
        objectLoader.accept(mv);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "ordinal", "()I", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putInt", "(I)Ljava/nio/ByteBuffer;", false);
        mv.visitInsn(Opcodes.POP);
    }

    @Override
    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex, Map<Object, FieldNode> constants, String className) {
        //Get values array
        FieldNode field = constants.computeIfAbsent(this.values, f -> {
            String name = "const_" + constants.size();
            return new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, "[" + this.getJavaType().getDescriptor(), null, null);
        });
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        //Stack: [this]
        mv.visitFieldInsn(Opcodes.GETFIELD, className, field.name, field.desc);
        //Stack: [values]
        mv.visitVarInsn(Opcodes.ASTORE, baseVarIndex);
        //Stack: []

        //Get ordinal
        bufferLoader.accept(mv);
        startPosLoader.accept(mv);
        //Stack: [buffer, startPos]
        ASMUtil.visitIntConstant(mv, baseOffset);
        //Stack: [buffer, startPos, baseOffset]
        mv.visitInsn(Opcodes.IADD);
        //Stack: [buffer, startPos + baseOffset]
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "getInt", "(I)I", false);
        //Stack: [ordinal]
        mv.visitVarInsn(Opcodes.ISTORE, baseVarIndex + 1);
        //Stack: []

        //Get value
        Label notNull = new Label();
        Label end = new Label();
        mv.visitVarInsn(Opcodes.ILOAD, baseVarIndex + 1);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, notNull);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitJumpInsn(Opcodes.GOTO, end);

        mv.visitLabel(notNull);
        mv.visitVarInsn(Opcodes.ALOAD, baseVarIndex);
        mv.visitVarInsn(Opcodes.ILOAD, baseVarIndex + 1);
        mv.visitInsn(Opcodes.AALOAD);

        mv.visitLabel(end);
    }

    @Override
    public void make(StringBuilder sb, Object obj, MalletContext context) {
        if (obj == null) {
            makeAny(sb, context);
            return;
        }

        Enum<?> enumObj = (Enum<?>) obj;

        sb.append(this.getName());
        sb.append("(");
        sb.append(enumObj.ordinal());
        sb.append(")");
    }

    @Override
    public void writeGLSLForGetField(StringBuilder sb, ObjectField field, ShaderCompiler shaderCompiler) {
        if (field.getType() == Type.INT_TYPE && field.getFieldName().equals("ordinal")) {
            getOrdinal(sb, field, shaderCompiler);
            return;
        }

        sb.append(this.getArrayName(field));
        sb.append("[");
        field.getObject().writeGLSL(sb, shaderCompiler.getGlobalContext(), shaderCompiler);
        sb.append("ordinal]");
    }

    private void getOrdinal(StringBuilder sb, ObjectField field, ShaderCompiler shaderCompiler) {
        sb.append("int(");
        field.getObject().writeGLSL(sb, shaderCompiler.getGlobalContext(), shaderCompiler);
        sb.append(".ordinal)");
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
    public boolean isGLSLPrimitive() {
        return false;
    }

    @Override
    public boolean isMalletPrimitive() {
        return true;
    }

    @Override
    public MalletType getTypeOfField(String field) {
        throw new IllegalStateException("Enum has no fields");
    }

    @Override
    public int getOffsetOfField(String field) {
        throw new IllegalStateException("Enum has no fields");
    }

    @Override
    public boolean isNullable() {
        return true;
    }

    @Override
    public void checkNullability(StringBuilder glsl, Value value) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static EnumType of(Class<? extends Enum<?>> clazz, MalletContext context) {
        return new EnumType(Type.getType(clazz), clazz.getEnumConstants(), context);
    }
}
