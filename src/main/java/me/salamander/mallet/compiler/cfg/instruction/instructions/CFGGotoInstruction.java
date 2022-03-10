package me.salamander.mallet.compiler.cfg.instruction.instructions;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.cfg.instruction.CFGNode;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.List;
import java.util.function.Function;

public class CFGGotoInstruction extends CFGSpecialInstruction {
    private CFGNode target;

    public CFGGotoInstruction(CFGNode target) {
        this.target = target;
    }

    @Override
    public void replaceTarget(CFGNode node, CFGNode replacement) {
        if (target == node) {
            target = replacement;
        }
    }

    @Override
    public List<Variable> usedVariables() {
        return List.of();
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new CFGGotoInstruction(target);
    }

    @Override
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        throw new IllegalStateException("Cannot write GLSL for CFGGotoInstruction");
    }

    @Override
    public String toString() {
        return "goto " + target.id;
    }
}
