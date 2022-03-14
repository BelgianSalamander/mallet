package me.salamander.mallet.shader;

import me.salamander.mallet.annotation.internal.ShaderInput;
import me.salamander.mallet.glsltypes.Vec3;
import me.salamander.mallet.glsltypes.Vec3i;

public abstract non-sealed class ComputeShader extends Shader {
    @ShaderInput(getter = "ivec3(gl_NumWorkGroups)")
    protected static final Vec3i gl_NumWorkGroups = null;
    @ShaderInput(getter = "ivec3(gl_WorkGroupID)")
    protected static final Vec3i gl_WorkGroupID = null;
    @ShaderInput(getter = "ivec3(gl-LocalInvocationID")
    protected static final Vec3i gl_LocalInvocationID = null;
    @ShaderInput(getter = "ivec3(gl_GlobalInvocationID)")
    protected static final Vec3i gl_GlobalInvocationID = null;
    @ShaderInput
    protected static final int gl_LocalInvocationIndex = 0;
}
