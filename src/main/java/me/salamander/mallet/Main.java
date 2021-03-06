package me.salamander.mallet;

import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.tests.ExampleVertexShader;
import org.joml.Vector3f;

public class Main {
    //TODO: Polymorphism
    public static void main(String[] args) {
        MalletContext context = new MalletContext();
        ShaderCompiler compiler = new ShaderCompiler(context, ExampleVertexShader.class);

        Vector3f tint = new Vector3f(1f, 0.5f, 0.5f);

        System.out.println(compiler.compile(
                new ExampleVertexShader.Func() {
                    @Override
                    public void run(Vector3f color) {
                        color.mul(tint);
                    }
                }
        ));

        System.out.println("Made GLSL!");
    }
}
