package me.salamander.mallet.compiler.ast.make.cfg.instructions;

import me.salamander.mallet.compiler.GlobalCompilationContext;
import me.salamander.mallet.compiler.ShaderCompiler;
import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.List;
import java.util.function.Function;

public class CFGJumpIfInstruction extends CFGSpecialInstruction {
    private CFGNode normal;
    private CFGNode ifTrue;
    private Value condition;
    private Instruction unresolvedNormal;

    public CFGJumpIfInstruction(CFGNode normal, CFGNode ifTrue, Value condition) {
        this.normal = normal;
        this.ifTrue = ifTrue;
        this.condition = condition;
    }

    @Override
    public void replaceTarget(CFGNode node, CFGNode replacement) {
        if (normal == node) {
            normal = replacement;
        }
        if (ifTrue == node) {
            ifTrue = replacement;
        }
    }

    @Override
    public List<Variable> usedVariables() {
        return condition.usedVariables();
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new CFGJumpIfInstruction(normal, ifTrue, valueCopier.apply(condition));
    }

    @Override
    public void writeGLSL(StringBuilder sb, GlobalCompilationContext ctx, ShaderCompiler shaderCompiler) {
        throw new IllegalStateException("Cannot write GLSL for CFGJumpIfInstruction");
    }

    @Override
    public String toString() {
        return "goto " + ifTrue.getId() + " if " + condition + " else " + normal.getId();
    }

    public CFGNode getNormal() {
        return normal;
    }

    public CFGNode getIfTrue() {
        return ifTrue;
    }

    public Value getCondition() {
        return condition;
    }

    public void setNormal(CFGNode normal) {
        this.normal = normal;
    }

    public void setUnresolvedNormal(Instruction unresolvedNormal) {
        this.unresolvedNormal = unresolvedNormal;
    }

    public Instruction getUnresolvedNormal() {
        return unresolvedNormal;
    }
}
