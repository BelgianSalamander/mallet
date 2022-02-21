package me.salamander.mallet.compiler.cfg.instruction.instructions;

import me.salamander.mallet.compiler.cfg.instruction.CFGNode;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;

import java.util.List;
import java.util.function.Function;

public abstract class CFGSpecialInstruction implements Instruction {
    public abstract void replaceTarget(CFGNode node, CFGNode replacement);
}
