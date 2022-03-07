package me.salamander.mallet;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.tests.ExampleVertexShader;
import org.joml.Vector3f;

public class Main {
    //TODO: Polymorphism
    public static void main(String[] args) {
        GlobalCompilationContext context = new GlobalCompilationContext();
        ShaderCompiler compiler = new ShaderCompiler(context, ExampleVertexShader.class);

        Vector3f tint = new Vector3f(1f, 0.5f, 0.5f);

        System.out.println(compiler.compile(
                (ExampleVertexShader.Func) (col) -> col.mul(tint)
        ));

        System.out.println("Made GLSL!");
    }
}
