package me.salamander.mallet.compiler.type;

import me.salamander.mallet.compiler.GlobalCompilationContext;
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

    public EnumType(Type type) {
        super(type);

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
    public void declareType(StringBuilder sb, GlobalCompilationContext context) {
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
    public void newType(StringBuilder sb, GlobalCompilationContext context) {
        throw new UnsupportedOperationException("Cannot create new enum");
    }

    @Override
    public void make(StringBuilder sb, Object obj, GlobalCompilationContext context) {
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
}
