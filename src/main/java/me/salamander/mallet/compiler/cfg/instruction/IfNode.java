package me.salamander.mallet.compiler.cfg.instruction;

import me.salamander.mallet.compiler.JavaDecompiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Code is very similat to LoopNode
public class IfNode /*extends CFGNode*/ {
    /*private InstructionCFG body;
    private List<CFGNode> exitPoints;

    protected IfNode(CFGNode jump, CFGNode start, Set<CFGNode> bodyNodes, InstructionCFG containing, JavaDecompiler decompiler, int id) {
        super(id);

        this.exitPoints = bodyNodes.stream()
                .filter(n -> !bodyNodes.containsAll(n.successors))
                .toList();

        this.body = new InstructionCFG(start, bodyNodes, containing.getIdCounter(), decompiler);

        containing.addNode(this);
        for(CFGNode predecessor: jump.predecessors) {
            predecessor.replaceSuccessor(jump, this);
        }

        for(CFGNode exitPoint: exitPoints) {
            for(CFGNode successor : exitPoint.successors) {
                if(!bodyNodes.contains(successor)) {
                    exitPoint.successors.remove(successor);
                    exitPoint.updateReachable();
                    successor.predecessors.remove(exitPoint);
                    successor.updateDominators();

                    this.successors.add(successor);
                    this.updateReachable();
                    successor.predecessors.add(this);
                    successor.updateDominators();
                }
            }
        }

        this.body.detectIfs();

        //Recompute successors
        Set<CFGNode> newSuccessors = new HashSet<>();
        Set<CFGNode> newExitPoints = new HashSet<>();
        for(CFGNode node : this.body.getNodes()) {
            for(CFGNode successor : node.successors) {
                if(!this.body.deepContains(successor)) {
                    newSuccessors.add(successor);
                    newExitPoints.add(node);
                }
            }
        }

        this.exitPoints = new ArrayList<>(newExitPoints);

        for(CFGNode successor : newSuccessors) {
            successor.predecessors.remove(this);
            successor.updateDominators();
        }

        this.successors.clear();
        this.successors.addAll(newSuccessors);
        this.updateReachable();
    }

    @Override
    public void addSuccessor(CFGNode successor) {
        throw new UnsupportedOperationException("Cannot add successors to an IfNode");
    }

    @Override
    public void replaceSuccessor(CFGNode oldSuccessor, CFGNode newSuccessor) {
        for(CFGNode exitPoint: exitPoints) {
            if (exitPoint.successors.contains(oldSuccessor)) {
                exitPoint.replaceSuccessor(oldSuccessor, newSuccessor);
            }
        }
    }

    public InstructionCFG getBody() {
        return body;
    }*/
}
