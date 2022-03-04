package me.salamander.mallet.util;

import it.unimi.dsi.fastutil.Hash;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Util {
    public static Hash.Strategy<Object> IDENTITY_HASH_STRATEGY = new Hash.Strategy<Object>() {
        public int hashCode(Object o) {
            return System.identityHashCode(o);
        }

        public boolean equals(Object o1, Object o2) {
            return o1 == o2;
        }
    };

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

    //Useful in debugging StackOverflowErrors
    public static boolean isCallStackLarge() {
        return Thread.currentThread().getStackTrace().length > 1000;
    }
}
