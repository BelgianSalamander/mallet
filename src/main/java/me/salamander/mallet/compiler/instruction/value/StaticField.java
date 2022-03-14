package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.annotation.In;
import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class StaticField implements Location, Value {
    private Type fieldOwner;
    private String fieldName;
    private Type fieldDesc;
    private final ShaderCompiler shaderCompiler;
    private Field cachedField = null;

    public StaticField(Type fieldOwner, String fieldName, Type fieldDesc, ShaderCompiler shaderCompiler) {
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
        this.shaderCompiler = shaderCompiler;
    }

    public Type getFieldOwner() {
        return fieldOwner;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Type getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldOwner(Type fieldOwner) {
        this.fieldOwner = fieldOwner;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFieldDesc(Type fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    @Override
    public Type getType() {
        return fieldDesc;
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return this.equals(value);
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of();
    }

    @Override
    public boolean allowInline() {
        return true;
    }

    @Override
    public boolean allowDuplicateInline() {
        return true;
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new StaticField(fieldOwner, fieldName, fieldDesc, shaderCompiler);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        shaderCompiler.getStatic(sb, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticField that = (StaticField) o;
        return Objects.equals(fieldOwner, that.fieldOwner) && Objects.equals(fieldName, that.fieldName) && Objects.equals(fieldDesc, that.fieldDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldOwner, fieldName, fieldDesc);
    }

    @Override
    public String toString() {
        return fieldOwner.getClassName() + "." + fieldName;
    }

    @Override
    public boolean canSet(MutabilityValue value) {
        return true; //THIS should only be true for shader globals
    }

    public Object getValue() {
        getField();
        try {
            return cachedField.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void getField() {
        if (cachedField == null) {
            try {
                Class<?> clazz = Class.forName(fieldOwner.getClassName());
                Field field = null;
                while (field == null && clazz != null) {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }

                if (field == null) {
                    throw new RuntimeException("Could not find field " + fieldName + " in class " + fieldOwner.getClassName());
                }

                field.setAccessible(true);

                cachedField = field;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean hasAnnotation(Class<? extends Annotation> clazz) {
        getField();

        return cachedField.isAnnotationPresent(clazz);
    }

    public <T extends Annotation> T getAnnotation(Class<T> clazz) {
        getField();

        return cachedField.getAnnotation(clazz);
    }
}
