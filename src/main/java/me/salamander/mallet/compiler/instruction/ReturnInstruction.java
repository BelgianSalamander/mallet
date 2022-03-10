package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReturnInstruction implements Instruction {
    private @Nullable Value value;

    public ReturnInstruction(@Nullable Value value) {
        this.value = value;
    }

    public @Nullable Value getValue() {
        return value;
    }

    public void setValue(@Nullable Value value) {
        this.value = value;
    }

    @Override
    public List<Integer> getNextIndices(Map<Label, Integer> labelIndices, int currentIndex) {
        return List.of(-1);
    }

    @Override
    public List<Variable> usedVariables() {
        if(value != null) {
            return value.usedVariables();
        }else{
            return List.of();
        }
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new ReturnInstruction(value != null ? valueCopier.apply(value) : null);
    }

    @Override
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        sb.append("return");
        if(value != null) {
            sb.append(" ");
            value.writeGLSL(sb, ctx, shaderCompiler);
        }
        sb.append(";\n");
    }

    @Override
    public String toString() {
        return "return " + (value != null ? value.toString() : "");
    }
}
