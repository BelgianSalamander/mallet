package me.salamander.mallet.shaders.compiler.cfg.instruction;

import java.util.ArrayList;
import java.util.List;

public class StartNode extends CFGNode {
    private CFGNode next;

    protected StartNode(int id, InstructionCFG parent, CFGNode next) {
        super(id, parent);
        this.next = next;

        this.successors.add(next);
        next.predecessors.add(this);
    }

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException("Cannot add successors to start node");
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        if(next == oldSuccessor) {
            next = newSuccessor;

            successors.clear();
            successors.add(newSuccessor);

            oldSuccessor.predecessors.remove(this);
            newSuccessor.predecessors.add(this);
        }
    }

    @Override
    protected String getDescription() {
        return "Start";
    }

    public List<CFGNode> getAllNodes() {
        List<CFGNode> nodes = new ArrayList<>();

        this.next.visitDepthFirstPreorder(nodes::add);

        return nodes;
    }

    public CFGNode getNext() {
        return next;
    }
}
