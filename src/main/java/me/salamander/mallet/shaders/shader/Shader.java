package me.salamander.mallet.shaders.shader;

public abstract sealed class Shader permits FragmentShader, VertexShader, ComputeShader {
    protected static <T> T copy(T obj){
        throw new AssertionError("COPY should not get called in JVM");
    }
}
