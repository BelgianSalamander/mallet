package me.salamander.mallet.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.annotation.NullableType;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructType extends MalletType {
    private final List<Field> fields;
    private final boolean nullable;

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
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getAlignment() {
        throw new UnsupportedOperationException();
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
