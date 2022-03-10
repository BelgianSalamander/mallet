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

public class StructType extends MalletType {
    private final List<Field> fields;

    public StructType(Type type) {
        super(type);

        List<Field> fields = new ArrayList<>();
        Class<?> clazz = Util.getClass(type);
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
    }

    @Override
    public void declareType(StringBuilder sb, GlobalCompilationContext context) {
        sb.append("struct ");
        sb.append(this.getName());
        sb.append(" {\n");

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
    public void make(StringBuilder sb, Object obj, GlobalCompilationContext context) {
        sb.append(this.getName());
        sb.append("(");

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
    public Set<Type> dependsOn() {
        Set<Type> dependsOn = new HashSet<>();

        for (Field field : fields) {
            dependsOn.add(Type.getType(field.getType()));
        }

        return dependsOn;
    }
}
