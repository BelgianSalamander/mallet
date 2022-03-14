package me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

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
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        throw new IllegalStateException("Goto instructions should not be written to GLSL");
    }

    @Override
    public String toString() {
        return "goto " + target.getId();
    }

    public CFGNode getTarget() {
        return target;
    }
}
