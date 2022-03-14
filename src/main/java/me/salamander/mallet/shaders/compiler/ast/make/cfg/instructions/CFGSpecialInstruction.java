package me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions;

import me.salamander.mallet.shaders.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;

public abstract class CFGSpecialInstruction implements Instruction {
    public abstract void replaceTarget(CFGNode node, CFGNode replacement);
}
