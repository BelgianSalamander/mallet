package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.ObjectField;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import java.util.Set;
import java.util.function.Consumer;

public abstract class MalletType {
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

    protected abstract int getSize();
    protected abstract int getAlignment();

    protected abstract void printLayout(StringBuilder sb, String indent);

    //Serialization
    /**
     * Returns a class that can be used to write an object of this type into a ByteBuffer.
     * @param itf The functional interface type to use. Should be a functional interface which takes two parameters: The ByteBuffer and the object to write. The reason this
     *            is not made a generic type is because Java doesn't support generic primitive types and so this can avoid boxing.
     * @param <T> The type of the functional interface. BiConsumer.class is guaranteed to work for every type.
     * @return A class that can be used to write an object of this type into a ByteBuffer.
     */
    public abstract <T> T makeWriter(Class<T> itf);

    /**
     * Assumes the position of the buffer is aligned
     * @param insns The instructions to add to
     */
    protected abstract void makeWriterCode(MethodVisitor mv, Consumer<MethodVisitor> bufferLoader, Consumer<MethodVisitor> objectLoader, int baseVarIndex);

    public void getField(StringBuilder sb, ObjectField field, ShaderCompiler shaderCompiler) {
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

    public abstract boolean isPrimitive();

    public boolean isNullable() {
        return false;
    }

    public void checkNullability(StringBuilder glsl, Value value) {
        throw new UnsupportedOperationException("checkNullability not implemented for " + this.getClass().getSimpleName());
    }
}
