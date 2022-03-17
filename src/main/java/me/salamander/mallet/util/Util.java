package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.Hash;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Util {
    public static final Unsafe UNSAFE;
    public static final String indent = "    ";
    public static Hash.Strategy<Object> IDENTITY_HASH_STRATEGY = new Hash.Strategy<Object>() {
        public int hashCode(Object o) {
            return System.identityHashCode(o);
        }

        public boolean equals(Object o1, Object o2) {
            return o1 == o2;
        }
    };

    public static Class<?> getClass(Type type) {
        if (type == Type.INT_TYPE) return int.class;
        if (type == Type.LONG_TYPE) return long.class;
        if (type == Type.DOUBLE_TYPE) return double.class;
        if (type == Type.FLOAT_TYPE) return float.class;
        if (type == Type.BOOLEAN_TYPE) return boolean.class;
        if (type == Type.BYTE_TYPE) return byte.class;
        if (type == Type.CHAR_TYPE) return char.class;
        if (type == Type.SHORT_TYPE) return short.class;
        if (type == Type.VOID_TYPE) return void.class;

        try {
            return Class.forName(type.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String removeSpecial(String s) {
        char startChar = s.charAt(0);
        if ('0' <= startChar && startChar <= '9') {
            s = "_" + s;
        }

        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @SafeVarargs
    public static <T> Set<T> union(Set<T>... sets) {
        Set<T> union = new HashSet<T>();
        for (Set<T> set : sets) {
            union.addAll(set);
        }
        return union;
    }

    public static <T> Set<T> intersection(Collection<Set<T>> sets) {
        if(sets.size() == 0) {
            return new HashSet<>();
        }else{
            Iterator<Set<T>> setsIterator = sets.iterator();

            Set<T> first = setsIterator.next();
            Set<T>[] setsArray = new Set[sets.size() - 1];

            int i = 0;
            while(setsIterator.hasNext()) {
                setsArray[i++] = setsIterator.next();
            }

            return intersection(first, setsArray);
        }
    }

    public static <T> Set<T> intersection(Set<T> first, Set<T>... sets) {
        Set<T> intersection = new HashSet<>(first);
        for (Set<T> set : sets) {
            intersection.retainAll(set);
        }
        return intersection;
    }

    public static <T> Set<T> makeSet(Iterable<T> original) {
        return StreamSupport.stream(original.spliterator(), false).collect(Collectors.toSet());
    }

    public static Object get(Field f, Object o) throws IllegalAccessException {
        if (f.getType() == boolean.class) {
            return f.getBoolean(o);
        } else if (f.getType() == byte.class) {
            return f.getByte(o);
        } else if (f.getType() == char.class) {
            return f.getChar(o);
        } else if (f.getType() == short.class) {
            return f.getShort(o);
        } else if (f.getType() == int.class) {
            return f.getInt(o);
        } else if (f.getType() == long.class) {
            return f.getLong(o);
        } else if (f.getType() == float.class) {
            return f.getFloat(o);
        } else if (f.getType() == double.class) {
            return f.getDouble(o);
        } else {
            return f.get(o);
        }
    }

    public static void align(ByteBuffer buffer, int alignment) {
        buffer.position(MathHelper.align(buffer.position(), alignment));
    }

    //Useful in debugging StackOverflowErrors
    public static boolean isCallStackLarge() {
        return Thread.currentThread().getStackTrace().length > 1000;
    }

    static {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
