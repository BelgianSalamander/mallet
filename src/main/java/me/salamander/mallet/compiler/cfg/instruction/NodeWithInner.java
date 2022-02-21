package me.salamander.mallet.compiler.cfg.instruction;

import java.util.Collection;

public abstract class NodeWithInner extends CFGNode {
    protected NodeWithInner(int id, InstructionCFG parent) {
        super(id, parent);
    }

    public abstract Collection<InnerCFGNode> innerCFGS();
}
