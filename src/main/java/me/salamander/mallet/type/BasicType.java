package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.glsltypes.Vec2;
import me.salamander.mallet.shaders.glsltypes.Vec3;
import me.salamander.mallet.shaders.glsltypes.Vec3i;
import me.salamander.mallet.shaders.glsltypes.Vec4;
import me.salamander.mallet.type.construct.ObjectConstructor;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Types that exist in GLSL
 */
public abstract class BasicType extends MalletType {
    private final String postfix;
    private final String defaultValue;
    private final int size;
    private final int alignment;
    private final Object writer;
    private final boolean isMalletPrimitive;

    protected BasicType(Type javaType, String glslName, String postfix, String defaultValue, MalletContext context, int size, int alignment, Object writer, boolean isMalletPrimitive) {
        super(javaType, glslName, context);
        this.postfix = postfix;
        this.defaultValue = defaultValue;
        this.size = size;
        this.alignment = alignment;
        this.writer = writer;
        this.isMalletPrimitive = isMalletPrimitive;
    }

    @Override
    public void declareType(StringBuilder sb, MalletContext context) {

    }

    @Override
    public void newType(StringBuilder sb, MalletContext context) {
        sb.append(defaultValue);
    }

    @Override
    public void make(StringBuilder sb, Object obj, MalletContext context) {
        sb.append(obj);
        sb.append(postfix);
    }

    @Override
    protected void makeAny(StringBuilder sb, MalletContext context) {
        newType(sb, context);
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
        sb.append(getName());
        sb.append("Size: ").append(getSize());
        sb.append(" Alignment: ").append(getAlignment());
    }

    @Override
    public  <T> T makeWriter(Class<T> itf) {
        if (itf.equals(BiConsumer.class)) {
            if (writer instanceof Boxable boxable) {
                return (T) boxable.box();
            }
        }

        return (T) writer;
    }

    @Override
    public Set<Type> dependsOn() {
        return Set.of();
    }

    @Override
    public boolean isGLSLPrimitive() {
        return true;
    }

    @Override
    public MalletType getTypeOfField(String field) {
        throw new IllegalStateException("Cannot get type of field " + field + " on " + getName());
    }

    @Override
    public int getOffsetOfField(String field) {
        throw new IllegalStateException("Cannot get offset of field " + field + " on " + getName());
    }

    @Override
    public boolean isMalletPrimitive() {
        return isMalletPrimitive;
    }

    public static BasicType[] makeTypes(MalletContext context) {
        BasicType int_ = new BasicType(Type.INT_TYPE, "int", "", "0", context, 4, 4, IntWriter.INSTANCE, true){
            @Override
            protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                bufferLoader.accept(mv);
                objectLoader.accept(mv);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putInt", "(I)Ljava/nio/ByteBuffer;", false);
            }

            @Override
            protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                bufferLoader.accept(mv);
                startPosLoader.accept(mv);
                ASMUtil.visitIntConstant(mv, baseOffset);
                mv.visitInsn(Opcodes.IADD);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "getInt", "(I)I", false);
            }
        };

        BasicType float_ = new BasicType(Type.FLOAT_TYPE, "float", "", "0.0f", context, 4, 4, FloatWriter.INSTANCE, true){
            @Override
            protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                bufferLoader.accept(mv);
                objectLoader.accept(mv);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putFloat", "(F)Ljava/nio/ByteBuffer;", false);
            }

            @Override
            protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                bufferLoader.accept(mv);
                startPosLoader.accept(mv);
                ASMUtil.visitIntConstant(mv, baseOffset);
                mv.visitInsn(Opcodes.IADD);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "getFloat", "(I)F", false);
            }
        };

        return new BasicType[] {
                int_,
                float_,
                new BasicType(Type.DOUBLE_TYPE, "double", "", "0.0", context, 8, 8, DoubleWriter.INSTANCE, true){
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "putDouble", "(D)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "getDouble", "(I)D", false);
                    }
                },
                new BasicType(Type.BOOLEAN_TYPE, "bool", "", "false", context, 1, 1, BooleanWriter.INSTANCE, true){
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "put", "(B)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "get", "(I)B", false);
                    }
                },
                new BasicType(Type.getType(Vec2.class), "vec2", "", "vec2(0.0f, 0.0f)", context, 8, 8, Vec2Writer.INSTANCE, false) {
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec2.class.getName().replace('.', '/'), "x", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec2.class.getName().replace('.', '/'), "y", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        mv.visitTypeInsn(Opcodes.NEW, Vec2.class.getName().replace('.', '/'));
                        mv.visitInsn(Opcodes.DUP);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 4);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec2.class.getName().replace('.', '/'), "<init>", "(FF)V", false);
                    }

                    @Override
                    public MalletType getTypeOfField(String field) {
                        return float_;
                    }

                    @Override
                    public int getOffsetOfField(String field) {
                        if (field.equals("x")) {
                            return 0;
                        } else if (field.equals("y")) {
                            return 4;
                        } else {
                            throw new IllegalArgumentException("Unknown field: " + field);
                        }
                    }
                },
                new BasicType(Type.getType(Vec3.class), "vec3", "", "vec3(0.0f, 0.0f, 0.0f)", context, 16, 16, Vec3Writer.INSTANCE, false) {
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3.class.getName().replace('.', '/'), "x", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3.class.getName().replace('.', '/'), "y", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3.class.getName().replace('.', '/'), "z", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        mv.visitTypeInsn(Opcodes.NEW, Vec3.class.getName().replace('.', '/'));
                        mv.visitInsn(Opcodes.DUP);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 4);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 8);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3.class.getName().replace('.', '/'), "<init>", "(FFF)V", false);
                    }

                    @Override
                    public MalletType getTypeOfField(String field) {
                        return float_;
                    }

                    @Override
                    public int getOffsetOfField(String field) {
                        if (field.equals("x")) {
                            return 0;
                        } else if (field.equals("y")) {
                            return 4;
                        } else if (field.equals("z")) {
                            return 8;
                        } else {
                            throw new IllegalArgumentException("Unknown field: " + field);
                        }
                    }
                },
                new BasicType(Type.getType(Vec4.class), "vec4", "", "vec4(0.0f, 0.0f, 0.0f, 0.0f)", context, 16, 16, Vec4Writer.INSTANCE, false) {
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec4.class.getName().replace('.', '/'), "x", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec4.class.getName().replace('.', '/'), "y", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec4.class.getName().replace('.', '/'), "z", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec4.class.getName().replace('.', '/'), "w", "()F", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putFloat", "(Ljava/nio/ByteBuffer;F)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 4);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 8);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 12);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getFloat", "(Ljava/nio/ByteBuffer;I)F", false);

                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec4.class.getName().replace('.', '/'), "<init>", "(FFFF)V", false);
                    }

                    @Override
                    public MalletType getTypeOfField(String field) {
                        return float_;
                    }

                    @Override
                    public int getOffsetOfField(String field) {
                        if (field.equals("x")) {
                            return 0;
                        } else if (field.equals("y")) {
                            return 4;
                        } else if (field.equals("z")) {
                            return 8;
                        } else if (field.equals("w")) {
                            return 12;
                        } else {
                            throw new IllegalArgumentException("Unknown field: " + field);
                        }
                    }
                },
                new BasicType(Type.getType(Vec3i.class), "ivec3", "", "ivec3(0, 0, 0)", context, 16, 16, Vec3iWriter.INSTANCE, false) {
                    @Override
                    protected void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3i.class.getName().replace('.', '/'), "x", "()I", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putInt", "(Ljava/nio/ByteBuffer;I)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3i.class.getName().replace('.', '/'), "y", "()I", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putInt", "(Ljava/nio/ByteBuffer;I)Ljava/nio/ByteBuffer;", false);

                        bufferLoader.accept(mv);
                        objectLoader.accept(mv);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3i.class.getName().replace('.', '/'), "z", "()I", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "putInt", "(Ljava/nio/ByteBuffer;I)Ljava/nio/ByteBuffer;", false);
                    }

                    @Override
                    protected void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex) {
                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getInt", "(Ljava/nio/ByteBuffer;I)I", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 4);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getInt", "(Ljava/nio/ByteBuffer;I)I", false);

                        bufferLoader.accept(mv);
                        startPosLoader.accept(mv);
                        ASMUtil.visitIntConstant(mv, baseOffset + 8);
                        mv.visitInsn(Opcodes.IADD);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/ByteBuffer", "getInt", "(Ljava/nio/ByteBuffer;I)I", false);

                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Vec3i.class.getName().replace('.', '/'), "<init>", "(III)V", false);
                    }

                    @Override
                    public MalletType getTypeOfField(String field) {
                        return int_;
                    }

                    @Override
                    public int getOffsetOfField(String field) {
                        if (field.equals("x")) {
                            return 0;
                        } else if (field.equals("y")) {
                            return 4;
                        } else if (field.equals("z")) {
                            return 8;
                        } else {
                            throw new IllegalArgumentException("Unknown field: " + field);
                        }
                    }
                },
        };
    }

    private interface Boxable {
        BiConsumer<ByteBuffer, Object> box();
    }

    @FunctionalInterface
    public interface IntWriter extends Boxable {
        IntWriter INSTANCE = (b, value) -> {
            Util.align(b, 4);
            b.putInt(value);
        };

        void write(ByteBuffer b, int value);

        default BiConsumer<ByteBuffer, Object> box() {
            return (b, value) -> write(b, (int) value);
        }
    }

    @FunctionalInterface
    public interface FloatWriter extends Boxable {
        FloatWriter INSTANCE = (b, value) -> {
            Util.align(b, 4);
            b.putFloat(value);
        };

        void write(ByteBuffer b, float value);

        default BiConsumer<ByteBuffer, Object> box() {
            return (b, value) -> write(b, (float) value);
        }
    }

    @FunctionalInterface
    public interface DoubleWriter extends Boxable {
        DoubleWriter INSTANCE = (b, value) -> {
            Util.align(b, 8);
            b.putDouble(value);
        };
        void write(ByteBuffer b, double value);

        default BiConsumer<ByteBuffer, Object> box() {
            return (b, value) -> write(b, (double) value);
        }
    }

    @FunctionalInterface
    public interface BooleanWriter extends Boxable {
        BooleanWriter INSTANCE = (b, value) -> {
            Util.align(b, 1);
            b.put((byte) (value ? 1 : 0));
        };
        void write(ByteBuffer b, boolean value);

        default BiConsumer<ByteBuffer, Object> box() {
            return (b, value) -> write(b, (boolean) value);
        }
    }

    @FunctionalInterface
    public interface Vec2Writer extends BiConsumer<ByteBuffer, Object> {
        Vec2Writer INSTANCE = (b, value) -> {
            Util.align(b, 8);
            b.putFloat(value.x());
            b.putFloat(value.y());
        };
        void write(ByteBuffer b, Vec2 value);

        @Override
        default void accept(ByteBuffer buffer, Object vec2) {
            write(buffer, (Vec2) vec2);
        }
    }

    @FunctionalInterface
    public interface Vec3Writer extends BiConsumer<ByteBuffer, Object> {
        Vec3Writer INSTANCE = (b, value) -> {
            Util.align(b, 16);
            b.putFloat(value.x());
            b.putFloat(value.y());
            b.putFloat(value.z());
        };
        void write(ByteBuffer b, Vec3 value);

        @Override
        default void accept(ByteBuffer buffer, Object vec3) {
            write(buffer, (Vec3) vec3);
        }
    }

    @FunctionalInterface
    public interface Vec4Writer extends BiConsumer<ByteBuffer, Object> {
        Vec4Writer INSTANCE = (b, value) -> {
            Util.align(b, 16);
            b.putFloat(value.x());
            b.putFloat(value.y());
            b.putFloat(value.z());
            b.putFloat(value.w());
        };
        void write(ByteBuffer b, Vec4 value);

        default void accept(ByteBuffer buffer, Object vec4) {
            write(buffer, (Vec4) vec4);
        }
    }

    @FunctionalInterface
    public interface Vec3iWriter extends BiConsumer<ByteBuffer, Object> {
        Vec3iWriter INSTANCE = (b, value) -> {
            Util.align(b, 16);
            b.putInt(value.x());
            b.putInt(value.y());
            b.putInt(value.z());
        };
        void write(ByteBuffer b, Vec3i value);

        default void accept(ByteBuffer buffer, Object vec3i) {
            write(buffer, (Vec3i) vec3i);
        }
    }
}
