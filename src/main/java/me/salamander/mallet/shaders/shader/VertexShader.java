package me.salamander.mallet.shaders.shader;

import me.salamander.mallet.shaders.annotation.internal.ShaderVar;
import me.salamander.mallet.shaders.glsltypes.Vec4;

public abstract non-sealed class VertexShader extends Shader {
    @ShaderVar
    protected static final int gl_VertexID = 0;

    @ShaderVar
    protected static Vec4 gl_Position;

    @ShaderVar
    protected static float gl_PointSize;

    @ShaderVar
    protected static float[] gl_ClipDistance;
}
