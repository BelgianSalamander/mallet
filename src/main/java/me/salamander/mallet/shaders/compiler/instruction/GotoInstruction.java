package me.salamander.mallet.shaders.compiler.instruction;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GotoInstruction implements Instruction{
    private Label target;

    public GotoInstruction(Label target) {
        this.target = target;
    }

    public Label getTarget() {
        return target;
    }

    public void setTarget(Label target) {
        this.target = target;
    }

    @Override
    public List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex) {
        return List.of(labelIndices.get(target));
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of();
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new GotoInstruction(target);
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        throw new IllegalStateException("GotoInstruction should not be written to GLSL");
    }

    @Override
    public String toString() {
        return "goto " + target;
    }
}
