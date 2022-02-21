package me.salamander.mallet.compiler.cfg.instruction.instructions;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.salamander.mallet.compiler.cfg.instruction.CFGNode;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class CFGSwitchInstruction extends CFGSpecialInstruction {
    private Value value;
    private final Int2ObjectMap<CFGNode> targets = new Int2ObjectOpenHashMap<>();
    private @Nullable CFGNode defaultTarget;

    public CFGSwitchInstruction(Value value) {
        this.value = value;
    }

    private CFGSwitchInstruction(Value value, Int2ObjectMap<CFGNode> targets, @Nullable CFGNode defaultTarget) {
        this.value = value;
        this.targets.putAll(targets);
        this.defaultTarget = defaultTarget;
    }

    public void setDefaultTarget(CFGNode defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public void addTarget(int key, CFGNode target) {
        targets.put(key, target);
    }

    @Override
    public void replaceTarget(CFGNode node, CFGNode replacement) {
        if(defaultTarget == node) {
            defaultTarget = replacement;
        }

        for(int key : targets.keySet()) {
            if(targets.get(key) == node) {
                targets.put(key, replacement);
            }
        }
    }

    @Override
    public List<Variable> usedVariables() {
        return value.usedVariables();
    }

    @Override
    public Instruction copy(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new CFGSwitchInstruction(valueCopier.apply(value), this.targets, this.defaultTarget);
    }
}
