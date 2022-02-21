package me.salamander.mallet.compiler.cfg.instruction;

import java.util.*;

public class LoopNode extends NodeWithInner {
    private CFGNode entryPoint;
    private InnerCFGNode body;

    public LoopNode(CFGNode entryPoint, Set<CFGNode> bodyNodes, InstructionCFG containing, int id) {
        super(id, containing);
        this.entryPoint = entryPoint;

        this.body = containing.groupNodes(bodyNodes);

        this.updateDominators();
        this.updateReachable();

        this.body.getCFG().detectLoops(entryPoint);

        System.out.println("Created loop node");
        this.printInfo(System.out);
    }

    @Override
    public Collection<InnerCFGNode> innerCFGS() {
        return List.of(body);
    }

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException("Cannot add a successor to a loop node");
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        this.body.replaceSuccessor(oldSuccessor, newSuccessor);

        this.successors.remove(oldSuccessor);
        oldSuccessor.predecessors.remove(this);
        oldSuccessor.updateDominators();

        this.successors.add(newSuccessor);
        newSuccessor.predecessors.add(this);
        newSuccessor.updateDominators();

        this.updateReachable();
    }
}
