package me.salamander.mallet.compiler.ast.make;

import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.compiler.ast.make.cfg.SpecialEdge;
import me.salamander.mallet.compiler.ast.make.cfg.SubGraph;
import me.salamander.mallet.compiler.ast.make.cfg.instructions.CFGGotoInstruction;
import me.salamander.mallet.compiler.ast.make.cfg.instructions.CFGJumpIfInstruction;
import me.salamander.mallet.compiler.ast.make.set.*;
import me.salamander.mallet.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.compiler.instruction.value.UnaryOperation;
import me.salamander.mallet.compiler.instruction.value.Value;
import org.graphstream.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class AbstractSyntaxTreeMaker {
    private final ControlFlowGraph cfg;
    private final StructureEncapsulationTree set;

    public AbstractSyntaxTreeMaker (IntermediaryCFG iCFG) {
        this.cfg = new ControlFlowGraph(iCFG);
        this.set = new StructureEncapsulationTree(this.cfg);
    }

    public ASTNode make() {
        makeLoops();
        makeIfs();
        //TODO: Switch statements
        makeStatementSequences();
        makeLabelledBlocks();

        findContinues();
        findBreaks();

        this.set.print(System.out);

        Graph graph = cfg.display();

        /*Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Show ID:");
            int showID = scanner.nextInt();

            if(showID == -1) break;

            for (CFGNode node : cfg) {
                graph.getNode(node.nodeName()).setAttribute("ui.style", "fill-color: gray;");
            }

            for (CFGNode node : set.getNode(showID).getBody()) {
                graph.getNode(node.nodeName()).setAttribute("ui.style", "fill-color: red;");
            }
        }*/

        List<ASTNode> ast = this.set.getRoot().makeAST(cfg);

        if (ast.size() == 1) {
            ASTNode root = ast.get(0);

            while(root.trySimplify() != null) {

            }

            return root;
        }

        throw new RuntimeException("Multiple AST nodes returned from SET root!");
    }

    private void findBreaks() {
        findBreaks(this.set.getRoot(), new HashMap<>());
    }

    private void findBreaks(SETNode node, Map<CFGNode, SETNode> breaks) {
        Map<CFGNode, SETNode> newBreaks = new HashMap<>(breaks);

        if (node instanceof LoopSetNode loop) {
            CFGNode exit = loop.getSortSuccessor();
            newBreaks.put(exit, node);
        }else if (node instanceof LabelledBlockSetNode labelledBlock) {
            CFGNode exit = labelledBlock.getExit().getEntryPoint();
            newBreaks.put(exit, node);
        }

        for (SETNode child : node.getChildren()) {
            if(child instanceof StatementSequenceSETNode sequence) {
                for (CFGNode cfgNode : sequence.getBody()) {
                    for (CFGNode successor : cfgNode.getSuccessors()) {
                        if (newBreaks.containsKey(successor)) {
                            if (!cfg.hasSpecialEdge(cfgNode, successor)) {
                                cfg.putSpecialEdge(cfgNode, successor, new SpecialEdge(newBreaks.get(successor), SpecialEdge.Type.BREAK));
                                System.out.println("Found break from! (" + cfgNode.getId() + " -> " + successor.getId() + ")");
                            }
                        }
                    }
                }

                continue;
            }

            findBreaks(child, newBreaks);
        }
    }

    private void findContinues() {
        findContinues(this.set.getRoot());
    }

    private void findContinues(SETNode node) {
        for (SETNode child : node.getChildren()) {
            if(child instanceof StatementSequenceSETNode) continue;

            findContinues(child);
        }

        if (node instanceof LoopSetNode loop) {
            CFGNode target = loop.getEntryPoint();

            for (CFGNode predecessor : target.getPredecessors()) {
                if (node.getBody().contains(predecessor)) {
                    cfg.putSpecialEdge(predecessor, target, new SpecialEdge(loop, SpecialEdge.Type.CONTINUE));
                    System.out.println("Found continue! (" + predecessor.getId() + " -> " + target.getId() + ")");
                }
            }
        }
    }

    private void makeLabelledBlocks() {
        this.makeLabelledBlocks(this.set.getRoot());
    }

    private void makeLabelledBlocks(SETNode setNode) {
        setNode.makeLabelledBlocks();

        for (SETNode child : setNode.getChildren()) {
            if(child instanceof StatementSequenceSETNode) continue;

            makeLabelledBlocks(child);
        }
    }

    private void makeStatementSequences() {
        makeStatementSequences(this.set.getRoot());

        this.set.print(System.out);
    }

    private void makeStatementSequences(SETNode setNode) {
        for (Set<CFGNode> subBody: setNode.getSubBodies()) {
            Set<CFGNode> unHandled = new HashSet<>(subBody);

            for (SETNode child : setNode.getChildren()) {
                unHandled.removeAll(child.getBody());
            }

            makeStatementSequences(unHandled);
        }

        for (SETNode child : setNode.getChildren()) {
            if(child instanceof StatementSequenceSETNode) continue;
            makeStatementSequences(child);
        }
    }

    private void makeStatementSequences(Set<CFGNode> nodes) {
        Set<CFGNode> heads = nodes.stream().filter(n -> Collections.disjoint(nodes, n.getPredecessors()) || n.getPredecessors().size() > 1).collect(Collectors.toSet());

        //Make sequence for each head
        for (CFGNode head : heads) {
            List<CFGNode> sequence = new ArrayList<>();

            while (true) {
                sequence.add(head);

                if(head.getSuccessors().size() != 1) break;
                head = head.getSuccessors().iterator().next();

                if(!nodes.contains(head) || head.getPredecessors().size() > 1) break;
            }

            StatementSequenceSETNode sequenceNode = new StatementSequenceSETNode(this.set, sequence);
            this.set.addNode(sequenceNode);
        }
    }

    // Ifs

    private void makeIfs() {
        for (CFGNode node : this.cfg) {
            if(node.getInstruction() instanceof CFGJumpIfInstruction jif) {
                if(jif.getIfTrue().canReach(jif.getNormal(), node)) {
                    makeIf(node, jif, jif.getIfTrue(), jif.getNormal(), false);
                } else if(jif.getNormal().canReach(jif.getIfTrue(), node)) {
                    makeIf(node, jif, jif.getNormal(), jif.getIfTrue(), true);
                } else {
                    makeIfElse(node, jif, jif.getIfTrue(), jif.getNormal());
                }
            }
        }
    }

    private void makeIfElse(CFGNode node, CFGJumpIfInstruction jif, CFGNode ifTrue, CFGNode normal) {
        Value condition = jif.getCondition();

        Set<CFGNode> SETTrim = node.SETParent.getBodyFor(node);

        Set<CFGNode> ifTrueBody = makeIfBody(ifTrue, SETTrim);
        Set<CFGNode> ifFalseBody = makeIfBody(normal, SETTrim);

        IfElseSetNode ifElseSetNode = new IfElseSetNode(this.set, node, condition, ifTrueBody, ifFalseBody);
        this.set.addNode(ifElseSetNode);
    }

    private void makeIf(CFGNode node, CFGJumpIfInstruction jif, CFGNode branchHead, CFGNode join, boolean invert) {
        Value condition = jif.getCondition();

        if(invert) {
            condition = new UnaryOperation(condition, UnaryOperation.Op.NOT);
        }

        Set<CFGNode> SETTrim = node.SETParent.getBodyFor(node);

        Set<CFGNode> body = makeIfBody(branchHead, SETTrim);

        IfSetNode ifSetNode = new IfSetNode(this.set, node, condition, body);
        this.set.addNode(ifSetNode);
    }

    private Set<CFGNode> makeIfBody(CFGNode branchHead, Set<CFGNode> trim) {
        Set<CFGNode> body = new HashSet<>();
        Stack<CFGNode> stack = new Stack<>();

        stack.push(branchHead);

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(!trim.contains(node)) {
                continue;
            }

            if(!node.getDominators().contains(branchHead)) {
                continue;
            }

            if(!body.add(node)) {
                continue;
            }

            node.getSuccessors().forEach(stack::push);
        }

        return body;
    }


    // Loops

    private void makeLoops() {
        makeLoops(cfg.full());
    }

    private void makeLoops(SubGraph subGraph) {
        Set<SubGraph> sccs = subGraph.findStronglyConnectedComponents();

        for(SubGraph scc : sccs) {
            this.makeLoop(scc);
        }
    }

    private void makeLoop(SubGraph subGraph) {
        //Calculate loop body
        List<CFGNode> potentialEntryPoints = subGraph.getMembers().stream().filter(node -> !subGraph.getMembers().containsAll(node.getPredecessors())).toList();

        if(potentialEntryPoints.size() != 1) {
            throw new IllegalArgumentException("LoopSetNode must have exactly one entry point. (Found " + potentialEntryPoints.size() + ")");
        }

        CFGNode entryPoint = potentialEntryPoints.get(0);

        CFGNode escapingSuccessor = null;

        if(entryPoint.getInstruction() instanceof CFGJumpIfInstruction jif) {
            CFGNode ifTrue = jif.getIfTrue();
            CFGNode normal = jif.getNormal();

            if(subGraph.getMembers().contains(ifTrue) && !subGraph.getMembers().contains(normal)) {
                escapingSuccessor = normal;
            }else if(subGraph.getMembers().contains(normal) && !subGraph.getMembers().contains(ifTrue)) {
                escapingSuccessor = ifTrue;
            }
        }

        if(escapingSuccessor == null) {
            //For now choose random exit point

            Set<CFGNode> potentialEscapes = subGraph.getMembers().stream()
                    .map(node -> {
                        Set<CFGNode> successors = new HashSet<>(node.getSuccessors());
                        successors.removeAll(subGraph.getMembers());
                        return successors;
                    })
                    .collect(
                            HashSet::new,
                            AbstractCollection::addAll,
                            AbstractCollection::addAll
                    );

            if(potentialEscapes.size() == 0) {
                throw new IllegalArgumentException("LoopSetNode must have at least one exit point. (Found 0)");
            }

            escapingSuccessor = potentialEscapes.iterator().next();
        }

        if(escapingSuccessor.getInstruction() instanceof CFGGotoInstruction goto_) {
            escapingSuccessor = goto_.getTarget();
        }

        Set<CFGNode> escapersReachingSet = new HashSet<>();
        Stack<CFGNode> stack = new Stack<>();

        stack.push(escapingSuccessor);

        while(!stack.isEmpty()) {
            CFGNode node = stack.pop();

            if(node == entryPoint) continue;

            if(!escapersReachingSet.add(node)) continue;

            stack.addAll(node.getSuccessors());
        }

        Set<CFGNode> body = new HashSet<>(subGraph.getMembers());

        for(CFGNode node: this.cfg) {
            if(node.getDominators().contains(entryPoint) && !escapersReachingSet.contains(node)) {
                body.add(node);
            }
        }

        //Create loop
        LoopSetNode loopSetNode = new LoopSetNode(set, body, entryPoint);
        this.set.addNode(loopSetNode);
        System.out.println("Made loop!");

        //Find inner loops
        this.makeLoops(new SubGraph(this.cfg, body).without(entryPoint));
    }
}
