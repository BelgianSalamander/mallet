package me.salamander.mallet.compiler.extension;

import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.instruction.value.Value;

import java.util.Set;

public interface Callable {
    void call(StringBuilder sb, ShaderCompiler compiler, Value... args);
    Set<GLSLCode> dependencies();
}
