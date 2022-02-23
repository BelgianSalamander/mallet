package me.salamander.mallet.compiler.cfg.instruction;

import java.util.Collection;

public abstract class NodeWithInner extends CFGNode {
    protected NodeWithInner(int id, InstructionCFG parent) {
        super(id, parent);
    }

    public abstract Collection<InnerCFGNode> innerCFGS();

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        for(InnerCFGNode innerCFGNode : innerCFGS()) {
            innerCFGNode.replaceSuccessor(oldSuccessor, newSuccessor);
        }

        this.successors.remove(oldSuccessor);
        oldSuccessor.predecessors.remove(this);

        this.successors.add(newSuccessor);
        newSuccessor.predecessors.add(this);
    }
}
