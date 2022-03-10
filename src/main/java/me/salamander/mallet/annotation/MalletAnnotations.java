package me.salamander.mallet.annotation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MalletAnnotations {
    private static final Map<Class<? extends Annotation>, BiConsumer<StringBuilder, Annotation>> writers = new HashMap<>();

    public static void write(StringBuilder sb, Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();

        writers.getOrDefault(annotationType, (a, b) -> {}).accept(sb, annotation);
    }

    private static BiConsumer<StringBuilder, Annotation> basicWriter(String value) {
        return (sb, annotation) -> sb.append(value).append(" ");
    }

    static {
        writers.put(In.class, basicWriter("in"));
        writers.put(Out.class, basicWriter("out"));
        writers.put(Buffer.class, basicWriter("buffer"));
        writers.put(Uniform.class, basicWriter("uniform"));

        writers.put(Layout.class, (sb, annotation) -> {
            sb.append("layout(");

            Layout layout = (Layout) annotation;
            boolean needsComma = false;

            if (layout.location() != -1) {
                sb.append("location = ").append(layout.location());
                needsComma = true;
            }

            if (layout.index() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("index = ").append(layout.index());
                needsComma = true;
            }

            if (layout.component() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("component = ").append(layout.component());
                needsComma = true;
            }

            if (layout.binding() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("binding = ").append(layout.binding());
                needsComma = true;
            }

            if (layout.local_size_x() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("local_size_x = ").append(layout.local_size_x());
                needsComma = true;
            }

            if (layout.local_size_y() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("local_size_y = ").append(layout.local_size_y());
                needsComma = true;
            }

            if (layout.local_size_z() != -1) {
                if (needsComma) sb.append(", ");
                sb.append("local_size_z = ").append(layout.local_size_z());
                needsComma = true;
            }

            sb.append(") ");
        });
    }
}
