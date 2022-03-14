package me.salamander.mallet.annotation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MalletAnnotations {
    public static void writeLayout(StringBuilder sb, Layout layout, boolean std430) {
        sb.append("layout(");

        boolean needsComma = false;

        if (std430) {
            sb.append("std430");
            needsComma = true;
        }

        if (layout.location() != -1) {
            if (needsComma) sb.append(", ");
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
    }
}
