package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.ObjectField;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnumType extends MalletType{
    private final List<Field> fields = new ArrayList<>();
    private final Class<? extends Enum<?>> clazz;

    public EnumType(Type type, MalletContext context) {
        super(type, context);

        Class<?> clazz = Util.getClass(type);
        this.clazz = (Class<? extends Enum<?>>) clazz;

        if(!clazz.isEnum()) {
            throw new IllegalArgumentException("Type " + type + " is not an enum");
        }

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

            Enum<?>[] constants = clazz.getEnumConstants();

            for (int i = 0; i < constants.length; i++) {
                Enum<?> constant = constants[i];

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
    protected int getSize() {
        return 4;
    }

    @Override
    protected int getAlignment() {
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

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "ordinal", "()I", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putInt", "(I)Ljava/nio/ByteBuffer;", false);
    }

    @Override
    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz.getName().replace('.', '/'), "values", "()[" + clazz.getName().replace('.', '/'), false);

        bufferLoader.accept(mv);
        startPosLoader.accept(mv);
        mv.visitInsn(Opcodes.IADD);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "getInt", "()I", false);

        mv.visitInsn(Opcodes.AALOAD);
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
        //TODO:
    }
}
