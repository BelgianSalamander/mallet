package me.salamander.mallet.shaders.compiler.type;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnumType extends MalletType{
    private final List<Field> fields = new ArrayList<>();

    public EnumType(Type type, MalletContext context) {
        super(type, context);

        Class<?> clazz = Util.getClass(type);

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
        sb.append("uint ordinal;\n");

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
        throw new UnsupportedOperationException("Cannot create new enum");
    }

    @Override
    protected void makeAny(StringBuilder sb, MalletContext context) {
        sb.append(this.getName()).append("(-1");
        for (Field field : fields) {
            sb.append(", ");

            MalletType fieldType = context.getType(Type.getType(field.getType()));
            fieldType.makeAny(sb, context);
        }

        sb.append(")");
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

        try {
            for (Field value : fields) {
                sb.append(", ");

                MalletType field = context.getType(Type.getType(value.getType()));
                field.make(sb, value.get(obj), context);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        sb.append(")");
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

    @Override
    public boolean isNullable() {
        return true;
    }

    @Override
    public void checkNullability(StringBuilder glsl, Value value) {
        super.checkNullability(glsl, value);
    }
}
