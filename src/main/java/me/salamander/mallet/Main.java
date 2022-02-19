package me.salamander.mallet;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.tests.ExampleVertexShader;

public class Main {
    public static void main(String[] args) {
        GlobalCompilationContext context = new GlobalCompilationContext();
        ShaderCompiler compiler = new ShaderCompiler(context, ExampleVertexShader.class);

        System.out.println(compiler.compile());
    }
}
