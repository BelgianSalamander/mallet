package me.salamander.mallet.shaders.compiler.extension;

import me.salamander.mallet.shaders.compiler.ShaderCompiler;

import java.util.Set;

public interface GLSLCode {
    void write(StringBuilder sb, ShaderCompiler compiler);

    Set<GLSLCode> dependencies();
}