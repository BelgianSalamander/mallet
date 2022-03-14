package me.salamander.mallet.compiler.extension;

import me.salamander.mallet.compiler.ShaderCompiler;

import java.util.Set;

public interface GLSLCode {
    void write(StringBuilder sb, ShaderCompiler compiler);

    Set<GLSLCode> dependencies();
}