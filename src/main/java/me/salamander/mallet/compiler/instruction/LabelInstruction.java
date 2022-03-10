package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

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
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        throw new UnsupportedOperationException();
    }
}
