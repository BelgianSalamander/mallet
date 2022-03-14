package me.salamander.mallet.shaders.compiler.cfg.instruction;

import me.salamander.mallet.shaders.compiler.cfg.instruction.instructions.CFGJumpIfInstruction;

import java.io.PrintStream;
import java.util.*;

public class LoopNode extends NodeWithInner {
    private CFGNode entryPoint;
    private InnerCFGNode body;
    private CFGNode escapingSuccessor;

    public LoopNode(CFGNode entryPoint, Set<CFGNode> sccNodes, InstructionCFG containing, int id) {
        super(id, containing);
        this.entryPoint = entryPoint;

        //Dava thesis 3.1.7 (Loop Bodies)

        this.escapingSuccessor = getEscapingSuccessor(entryPoint, sccNodes);
        Set<CFGNode> escapersReachingSet = getEscapersReachingSet(entryPoint,this.escapingSuccessor);
        Set<CFGNode> body = calculateBody(entryPoint, sccNodes, escapersReachingSet);

        this.body = containing.groupNodes(body);

        this.body.getCFG().detectLoops(entryPoint);

        System.out.println("Created loop node");
    }

    private Set<CFGNode> calculateBody(CFGNode entryPoint, Set<CFGNode> sccNodes, Set<CFGNode> escapersReachingSet) {
        Set<CFGNode> body = new HashSet<>();
        Set<CFGNode> visited = new HashSet<>();

        Stack<CFGNode> stack = new Stack<>();

        for(CFGNode node: sccNodes) {
            body.add(node);
            visited.add(node);

            for(CFGNode successor: node.getAllSuccessors()) {
                if(!sccNodes.contains(successor)) {
                    stack.push(successor);
                }
            }
        }

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(escapersReachingSet.contains(node)) {
                continue;
            }

            if(visited.add(node)) {
                if(node.isDominatedBy(entryPoint)) {
                    body.add(node);

                    for(CFGNode successor: node.getAllSuccessors()) {
                        stack.push(successor);
                    }
                }
            }
        }

        return body;
    }

    private static Set<CFGNode> getEscapersReachingSet(CFGNode entryPoint, CFGNode escapingSuccessor) {
        Set<CFGNode> escapersReachingSet = new HashSet<>();

        Stack<CFGNode> stack = new Stack<>();
        stack.push(escapingSuccessor);

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(node == entryPoint) {
                continue;
            }

            if(escapersReachingSet.add(node)) {
                for (CFGNode successor : node.getAllSuccessors()) {
                    stack.push(successor);
                }
            }
        }

        return escapersReachingSet;
    }

    private static CFGNode getEscapingSuccessor(CFGNode entryPoint, Set<CFGNode> sccNodes) {
        if(entryPoint instanceof InstructionNode insnNode) {
            if(insnNode.instruction instanceof CFGJumpIfInstruction jumpIf) {
                boolean normalInBody = sccNodes.contains(jumpIf.getNormal());
                boolean ifTrueInBody = sccNodes.contains(jumpIf.getIfTrue());

                if(normalInBody && !ifTrueInBody) {
                    return jumpIf.getIfTrue();
                }else if(!normalInBody && ifTrueInBody) {
                    return jumpIf.getNormal();
                }
            }
        }

        Set<CFGNode> possibleEscapingSuccessors = new HashSet<>();

        for(CFGNode node: sccNodes) {
            possibleEscapingSuccessors.addAll(node.getAllSuccessors().stream().filter(s -> !sccNodes.contains(s)).toList());
        }

        CFGNode bestEscapingSuccessor = null;
        int longestShortestPath = -1;

        for(CFGNode possibleEscapingSuccessor: possibleEscapingSuccessors) {
            int shortestPath = possibleEscapingSuccessor.getShortestPathTo(CFGNode::isReturn);

            if(shortestPath > longestShortestPath) {
                bestEscapingSuccessor = possibleEscapingSuccessor;
                longestShortestPath = shortestPath;
            }
        }

        return bestEscapingSuccessor;
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

        this.successors.add(newSuccessor);
        newSuccessor.predecessors.add(this);
    }

    @Override
    protected String getDescription() {
        return "Loop";
    }

    @Override
    public void printInfo(PrintStream out) {
        System.out.println("Loop Node: " + this.id);
        System.out.println("Escaping Successor: " + this.escapingSuccessor.id);
        System.out.println("=== BODY START ===");
        this.body.getCFG().printInfo(out);
        System.out.println("=== BODY END (" + this.id + ") ===");

        System.out.println("Successors:");
        for(CFGNode successor: getAllSuccessors()) {
            System.out.println("\t" + successor.id);
        }
    }

    public InnerCFGNode getBody() {
        return body;
    }

    public CFGNode getEntryPoint() {
        return entryPoint;
    }
}
