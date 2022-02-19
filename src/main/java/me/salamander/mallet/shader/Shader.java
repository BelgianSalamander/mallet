package me.salamander.mallet.shader;

public abstract class Shader {
    protected static <T> T copy(T obj){
        throw new AssertionError("COPY should not get called in JVM");
    }
}
