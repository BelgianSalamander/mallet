package me.salamander.mallet.shader;

import me.salamander.mallet.annotation.internal.ShaderInput;
import me.salamander.mallet.glsltypes.Vec4;
import org.joml.Vector4f;

public abstract non-sealed class VertexShader extends Shader {
    @ShaderInput
    protected static Vec4 gl_Position;
}
