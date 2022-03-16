package me.salamander.mallet.util;

public class MathHelper {
    public static int align(int position, int alignment) {
        if (position % alignment != 0) {
            position += alignment - position % alignment;
        }

        return position;
    }
}
