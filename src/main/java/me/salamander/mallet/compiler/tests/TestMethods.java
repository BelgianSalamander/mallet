package me.salamander.mallet.compiler.tests;

import java.util.Map;

public class TestMethods {
    public static int test(boolean a, Map<Integer, Float> map) {
        map.get(a ? 1 : 2);

        TestMethods methods = new TestMethods();

        return 0;
    }
}
