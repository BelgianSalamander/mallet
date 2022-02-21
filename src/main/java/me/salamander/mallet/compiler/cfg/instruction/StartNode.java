package me.salamander.mallet.compiler.cfg.instruction;

public class StartNode extends CFGNode {
    private CFGNode next;

    protected StartNode(int id, InstructionCFG parent, CFGNode next) {
        super(id, parent);
        this.next = next;

        this.successors.add(next);
        next.predecessors.add(this);

        this.updateReachable();
        next.updateDominators();
    }

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException("Cannot add successors to start node");
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        if(next == oldSuccessor) {
            next = newSuccessor;
        }
    }
}
