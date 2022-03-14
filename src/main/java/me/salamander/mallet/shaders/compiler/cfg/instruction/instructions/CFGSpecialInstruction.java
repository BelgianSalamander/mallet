package me.salamander.mallet.shaders.compiler.cfg.instruction.instructions;

import me.salamander.mallet.shaders.compiler.cfg.instruction.CFGNode;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;

public abstract class CFGSpecialInstruction implements Instruction {
    public abstract void replaceTarget(CFGNode node, CFGNode replacement);
}
