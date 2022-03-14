package me.salamander.mallet.shaders.compiler.instruction;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.List;
import java.util.function.Function;

public class LabelInstruction implements Instruction {
    private final Label label;

    public LabelInstruction(Label label) {
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label + ":";
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of();
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new LabelInstruction(label);
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        throw new UnsupportedOperationException();
    }
}
