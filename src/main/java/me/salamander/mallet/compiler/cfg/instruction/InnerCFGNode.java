package me.salamander.mallet.compiler.cfg.instruction;

import java.util.List;
import java.util.Set;

public class InnerCFGNode extends CFGNode {
    private InstructionCFG cfg;

    protected InnerCFGNode(int id, InstructionCFG parent, InstructionCFG cfg) {
        super(id, parent);
        this.cfg = cfg;
    }

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        for(Set<CFGNode> externalConnections: cfg.getExternalConnections().values()) {
            if(externalConnections.contains(oldSuccessor)) {
                externalConnections.remove(oldSuccessor);
                externalConnections.add(newSuccessor);
            }
        }

        this.successors.remove(oldSuccessor);
        oldSuccessor.predecessors.remove(this);
        oldSuccessor.updateDominators();

        this.successors.add(newSuccessor);
        newSuccessor.predecessors.add(this);
        newSuccessor.updateDominators();

        this.updateReachable();
    }

    public InstructionCFG getCFG() {
        return cfg;
    }
}
