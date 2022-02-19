package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.Hash;

public class Util {
    public static final Hash.Strategy<Object> IDENTITY_HASH_STRATEGY = new Hash.Strategy<Object>() {
        public int hashCode(Object o) {
            return System.identityHashCode(o);
        }

        public boolean equals(Object o1, Object o2) {
            return o1 == o2;
        }
    };
}
