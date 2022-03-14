package me.salamander.mallet.shaders.shader;

import me.salamander.mallet.shaders.annotation.internal.ShaderVar;
import me.salamander.mallet.shaders.glsltypes.Vec3i;

public abstract non-sealed class ComputeShader extends Shader {
    @ShaderVar(getter = "ivec3(gl_NumWorkGroups)")
    protected static final Vec3i gl_NumWorkGroups = null;
    @ShaderVar(getter = "ivec3(gl_WorkGroupID)")
    protected static final Vec3i gl_WorkGroupID = null;
    @ShaderVar(getter = "ivec3(gl-LocalInvocationID")
    protected static final Vec3i gl_LocalInvocationID = null;
    @ShaderVar(getter = "ivec3(gl_GlobalInvocationID)")
    protected static final Vec3i gl_GlobalInvocationID = null;
    @ShaderVar
    protected static final int gl_LocalInvocationIndex = 0;
}
