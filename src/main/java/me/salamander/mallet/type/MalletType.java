package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.globject.vao.VAOLayout;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.ObjectField;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.MathHelper;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class MalletType {
    protected static final AtomicLong ID_COUNTER = new AtomicLong();

    private final Type javaType;
    private final String glslName;
    protected final MalletContext context;

    protected MalletType(Type javaType, MalletContext context) {
        this(javaType, Util.removeSpecial(javaType.getClassName()), context);
    }

    protected MalletType(Type javaType, String glslName, MalletContext context) {
        this.javaType = javaType;
        this.glslName = glslName;
        this.context = context;
    }

    public abstract void declareType(StringBuilder sb, MalletContext context);

    public abstract void newType(StringBuilder sb, MalletContext context);

    public abstract void make(StringBuilder sb, Object obj, MalletContext context);

    protected abstract void makeAny(StringBuilder sb, MalletContext context);

    public abstract int getSize();
    public abstract int getAlignment();

    protected abstract void printLayout(StringBuilder sb, String indent);

    //Serialization
    /**
     * Returns a class that can be used to write an object of this type into a ByteBuffer.
     * @param itf The functional interface type to use. Should be a functional interface which takes two parameters: The ByteBuffer and the object to write. The reason this
     *            is not made a generic type is because Java doesn't support generic primitive types and so this can avoid boxing.
     * @param <T> The type of the functional interface. Must be one of the following:
     *            <ul>{@link java.util.function.BiConsumer} (This is guaranteed to work for any type but may result in boxing)</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.IntWriter}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.FloatWriter}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.DoubleWriter}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.BooleanWriter}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.Vec2Writer}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.Vec3Writer}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.Vec4Writer}</ul>
     *            <ul>{@link me.salamander.mallet.type.BasicType.Vec3iWriter}</ul>
     * @return A class that can be used to write an object of this type into a ByteBuffer.
     */
    public abstract <T> T makeWriter(Class<T> itf);

    protected abstract void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex);

    /**
     * Creates a reader for this type.
     * @param itf A callback to use when an object is read.
     * @param args The arguments to pass to the functional interface. "this" will make it pass the whole object to the "this.x", "this.y" will make it pass the x and y.
     *             If x and y are integers then the functional interface should accept {@code int}s.
     * @param <T> A consumer-type interface that matches exactly the types of the arguments.
     * @return A function that can be used to read an object of this type.
     */
    public <T> BiConsumer<ByteBuffer, T> makeReader(Class<T> itf, String... args) {
        if (!itf.isInterface()) {
            throw new IllegalArgumentException("itf must be an interface");
        }

        //Get method
        Method method = null;
        //Find the only non-default method
        for (Method m : itf.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                if (method != null) {
                    throw new IllegalArgumentException("itf must have only one abstract method");
                }

                method = m;
            }
        }

        if (method == null) {
            throw new IllegalArgumentException("No valid method found in " + itf.getName());
        }

        String[][] fields = new String[args.length][];
        MalletType[] types = new MalletType[args.length];
        int[] offsets = new int[args.length];

        for (int i = 0; i < args.length; i++) {
            String[] field = args[i].split("\\.");
            fields[i] = Arrays.copyOfRange(field, 1, field.length);
        }

        for (int i = 0; i < args.length; i++) {
            String[] field = fields[i];
            MalletType curr = this;
            int offset = 0;

            for (String f : field) {
                offset += curr.getOffsetOfField(f);
                curr = curr.getTypeOfField(f);
            }

            types[i] = curr;
            offsets[i] = offset;
        }

        //Check types
        //Check type count
        if (method.getParameterCount() != args.length) {
            throw new IllegalArgumentException("Mismatched argument length. " + itf.getName() + "." + method.getName() + " expects " + method.getParameterCount() + " arguments, but " + args.length + " were provided");
        }

        //Check that types match
        //Get class signature's type arguments
        Map<String, Class<?>> classSignatureTypes = new HashMap<>();
        Map<String, MalletType> classSignatureMalletTypes = new HashMap<>();
        for (TypeVariable<Class<T>> typeParameter : itf.getTypeParameters()) {
            if (typeParameter.getBounds().length > 1) {
                throw new IllegalArgumentException("Type parameter " + typeParameter.getName() + " has more than one bound");
            }

            var bound = typeParameter.getBounds()[0];

            if (bound instanceof Class clazz) {
                classSignatureTypes.put(typeParameter.getName(), clazz);
            } else {
                throw new IllegalArgumentException("Type bounds must be concrete classes");
            }
        }

        if (method.getTypeParameters().length != 0) {
            throw new IllegalArgumentException("Method type parameters are not supported");
        }

        //Check that types match
        for (int i = 0; i < args.length; i++) {
            MalletType targetType = types[i];
            var actualType = method.getGenericParameterTypes()[i];

            if (actualType instanceof TypeVariable) {
                @SuppressWarnings("unchecked")
                TypeVariable<Class<T>> typeVariable = (TypeVariable<Class<T>>) actualType;
                String name = typeVariable.getName();

                if (classSignatureMalletTypes.containsKey(name)) {
                    if (!classSignatureMalletTypes.get(name).equals(targetType)) {
                        throw new IllegalArgumentException("Type parameter mismatch for " + name + ": " + classSignatureMalletTypes.get(name) + " != " + targetType);
                    }
                    continue;
                }

                //Check bound
                Class<?> bound = classSignatureTypes.get(name);
                Class<?> target;
                try {
                    target = Class.forName(targetType.getJavaType().getClassName());
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Could not find class " + targetType.getJavaType().getClassName());
                }

                if (!bound.isAssignableFrom(target)) {
                    //TODO: Automatic boxing
                    throw new IllegalArgumentException("Type parameter mismatch for " + name + ": " + target + " is not assignable from " + bound);
                }

                classSignatureMalletTypes.put(name, targetType);
            }
        }

        String name = "me/salamander/mallet/generated/MalletStructReader_" + ID_COUNTER.getAndIncrement();

        StringBuilder callbackSignature = new StringBuilder();
        callbackSignature.append("L");
        callbackSignature.append(itf.getName().replace('.', '/'));

        if (classSignatureTypes.size() > 0) {
            callbackSignature.append("<");
            for (TypeVariable<Class<T>> typeParameter : itf.getTypeParameters()) {
                String typeParameterName = typeParameter.getName();
                MalletType type = classSignatureMalletTypes.get(typeParameterName);
                callbackSignature.append(type.getJavaType().getDescriptor());
            }
            callbackSignature.append(">");
        }

        callbackSignature.append(";");

        String signature = "Ljava/lang/Object;Ljava/util/function/BiConsumer<Ljava/nio/ByteBuffer;" + callbackSignature + ">;";

        ClassNode classNode = new ClassNode();
        classNode.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC,
                name,
                signature,
                "java/lang/Object",
                new String[]{"java/util/function/BiConsumer"}
        );


        //Reader delegator
        createReaderDelegator(itf, name, classNode);

        //Actual reader
        MethodVisitor mv = classNode.visitMethod(
                Opcodes.ACC_PUBLIC,
                "accept",
                "(Ljava/nio/ByteBuffer;L" + itf.getName().replace('.', '/') + ";)V",
                null,
                null
        );

        mv.visitCode();

        //Align
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitIntInsn(Opcodes.BIPUSH, getAlignment());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "me/salamander/mallet/util/Util", "align", "(Ljava/nio/ByteBuffer;I)V", false);

        //Store start position
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "()I", false);
        mv.visitVarInsn(Opcodes.ISTORE, 3);

        //Load callback
        mv.visitVarInsn(Opcodes.ALOAD, 2);

        Map<Object, FieldNode> constants = new HashMap<>();

        //Create params
        for (int i = 0; i < args.length; i++) {
            MalletType type = types[i];
            int offset = offsets[i];

            type.makeReaderCode(
                    mv,
                    offset,
                    (mv1) -> mv1.visitVarInsn(Opcodes.ALOAD, 1),
                    (mv1) -> mv1.visitVarInsn(Opcodes.ILOAD, 3),
                    4,
                    constants,
                    name
            );
        }

        Type[] argTypes = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = Type.getType(method.getParameterTypes()[i]);
        }
        String methodDesc = Type.getMethodDescriptor(Type.VOID_TYPE, argTypes);

        //Call callback
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, itf.getName().replace('.', '/'), method.getName(), methodDesc, true);

        //Set position
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        ASMUtil.visitIntConstant(mv, getSize());
        mv.visitInsn(Opcodes.IADD);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;", false);

        mv.visitInsn(Opcodes.RETURN);

        //Create constructor
        Map.Entry<Object, FieldNode>[] entries = constants.entrySet().toArray(new Map.Entry[0]);
        Type[] constantTypes = new Type[entries.length];
        StringBuilder constructorDesc = new StringBuilder();

        constructorDesc.append("(");
        for (int i = 0; i < entries.length; i++) {
            Type type = Type.getType(entries[i].getKey().getClass());
            constantTypes[i] = type;
            constructorDesc.append(type.getDescriptor());
        }
        constructorDesc.append(")V");

        MethodVisitor constructor = classNode.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                constructorDesc.toString(),
                null,
                null
        );

        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        int varIndex = 1;
        for (int i = 0; i < entries.length; i++) {
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, varIndex);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, name, entries[i].getValue().name, entries[i].getValue().desc);
            varIndex += types[i].getSize();
        }

        constructor.visitInsn(Opcodes.RETURN);

        Object[] constructorArgs = new Object[entries.length];

        int i = 0;
        for (Map.Entry<Object, FieldNode> entry : entries) {
            classNode.fields.add(entry.getValue());
            constructorArgs[i++] = entry.getKey();
        }

        Class<?> clazz = ASMUtil.load(this.getClass().getClassLoader(), classNode)[0];
        try {
            Constructor<?> reflectionConstructor = clazz.getConstructors()[0];
            return (BiConsumer<ByteBuffer, T>) reflectionConstructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> void createReaderDelegator(Class<T> itf, String name, ClassNode classNode) {
        MethodVisitor mv = classNode.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                "accept",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
        );

        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, 0);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/nio/ByteBuffer");

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitTypeInsn(Opcodes.CHECKCAST, itf.getName().replace('.', '/'));

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, name, "accept", "(Ljava/nio/ByteBuffer;L" + itf.getName().replace('.', '/') + ";)V", false);
        mv.visitInsn(Opcodes.RETURN);
    }

    /**
     * Should read the object from the buffer and push the read value onto the stack.
     * @param mv
     * @param baseOffset
     * @param bufferLoader
     * @param baseVarIndex
     * @param constants
     * @param className
     */
    protected abstract void makeReaderCode(MethodVisitor mv, int baseOffset, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> startPosLoader, int baseVarIndex, Map<Object, FieldNode> constants, String className);

    public void writeGLSLForGetField(StringBuilder sb, ObjectField field, ShaderCompiler shaderCompiler) {
        field.getObject().writeGLSL(sb, context, shaderCompiler);
        sb.append('.');
        sb.append(field.getFieldName());
    }

    public abstract Set<Type> dependsOn();

    public String getName() {
        return glslName;
    }

    public Type getJavaType() {
        return javaType;
    }

    public abstract boolean isGLSLPrimitive();

    public abstract boolean isMalletPrimitive();

    public abstract MalletType getTypeOfField(String field);
    public abstract int getOffsetOfField(String field);

    public boolean isNullable() {
        return false;
    }

    public void checkNullability(StringBuilder glsl, Value value, ShaderCompiler compiler) {
        throw new UnsupportedOperationException("checkNullability not implemented for " + this.getClass().getSimpleName());
    }

    public VAOLayout createLayout() {
        VAOLayout.SingleBufferBuilder builder = new VAOLayout.SingleBufferBuilder(MathHelper.align(getSize(), getAlignment()));

        this.addToLayout(builder, 0);

        return builder.build();
    }

    public abstract void addToLayout(VAOLayout.SingleBufferBuilder builder, int pos);
}
