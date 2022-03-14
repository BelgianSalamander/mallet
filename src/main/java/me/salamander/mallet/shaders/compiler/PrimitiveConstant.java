package me.salamander.mallet.shaders.compiler;

import org.objectweb.asm.Type;

public record PrimitiveConstant(Object obj, boolean isPrimitive) {
    public Type getType() {
        if (!isPrimitive) {
            return Type.getType(obj.getClass());
        }

        if (obj instanceof Integer) {
            return Type.INT_TYPE;
        } else if (obj instanceof Long) {
            return Type.LONG_TYPE;
        } else if (obj instanceof Float) {
            return Type.FLOAT_TYPE;
        } else if (obj instanceof Double) {
            return Type.DOUBLE_TYPE;
        } else if (obj instanceof Character) {
            return Type.CHAR_TYPE;
        } else if (obj instanceof Boolean) {
            return Type.BOOLEAN_TYPE;
        } else if (obj instanceof Byte) {
            return Type.BYTE_TYPE;
        } else if (obj instanceof Short) {
            return Type.SHORT_TYPE;
        } else {
            throw new IllegalArgumentException("Unknown primitive type: " + obj.getClass());
        }
    }

    public static PrimitiveConstant of(Object obj) {
        return new PrimitiveConstant(obj, false);
    }

    public static PrimitiveConstant of(int value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(long value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(float value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(double value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(char value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(boolean value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(byte value) {
        return new PrimitiveConstant(value, true);
    }

    public static PrimitiveConstant of(short value) {
        return new PrimitiveConstant(value, true);
    }
}
