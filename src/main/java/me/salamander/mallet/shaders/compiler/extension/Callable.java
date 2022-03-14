package me.salamander.mallet.shaders.compiler.extension;

import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;

import java.util.Set;

public interface Callable {
    void call(StringBuilder sb, ShaderCompiler compiler, Value... args);
    Set<GLSLCode> dependencies();
}
