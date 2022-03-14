package me.salamander.mallet.shaders.compiler.ast.make.cfg;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions.CFGGotoInstruction;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions.CFGJumpIfInstruction;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions.CFGSpecialInstruction;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions.CFGSwitchInstruction;
import me.salamander.mallet.shaders.compiler.cfg.BasicBlock;
import me.salamander.mallet.shaders.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.shaders.compiler.instruction.*;
import me.salamander.mallet.util.Util;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ControlFlowGraph implements Iterable<CFGNode> {
    private final CFGNode root;
    private final Map<Instruction, CFGNode> instructionToNode = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger();
    private final Map<Edge, SpecialEdge> specialEdges = new HashMap<>();

    public ControlFlowGraph(IntermediaryCFG previous) {
        Map<Label, CFGNode> labelNodes = new HashMap<>();
        Set<Instruction> deferredJumpInstructions = new HashSet<>();

        Map<Instruction, Set<Instruction>> successors = new HashMap<>();

        for(BasicBlock block: previous.getBlocks()){
            //Create intra-block edges
            for (int i = 0; i < block.getInstructions().size() - 1; i++) {
                Instruction instruction = block.getInstructions().get(i);
                Instruction nextInstruction = block.getInstructions().get(i + 1);

                successors.computeIfAbsent(instruction, k -> new HashSet<>()).add(nextInstruction);
            }

            if(block.getInstructions().size() == 0) {
                //Is start or end block
                continue;
            }

            //Inter-block edges
            Instruction lastInstruction = block.getInstructions().get(block.getInstructions().size() - 1);

            for(BasicBlock successor: block.getNext()){
                if(successor.getInstructions().size() == 0){
                    //Is end block
                    continue;
                }

                Instruction firstInstruction = successor.getInstructions().get(0);

                successors.computeIfAbsent(lastInstruction, k -> new HashSet<>()).add(firstInstruction);
            }
        }

        for(BasicBlock block: previous.getBlocks()) {
            for (Instruction instruction: block.getInstructions()) {
                if (instruction instanceof GotoInstruction || instruction instanceof JumpIfInstruction || instruction instanceof SwitchInstruction) {
                    deferredJumpInstructions.add(instruction);
                    continue;
                }

                CFGNode node = new CFGNode(instruction, nextID());
                instructionToNode.put(instruction, node);

                if (instruction instanceof LabelInstruction labelInsn) {
                    labelNodes.put(labelInsn.getLabel(), node);
                }
            }
        }

        Set<CFGJumpIfInstruction> unresolvedIfs = new HashSet<>();
        for(Instruction instruction: deferredJumpInstructions) {
            CFGSpecialInstruction newInsn = null;

            if(instruction instanceof GotoInstruction goto_) {
                newInsn = new CFGGotoInstruction(labelNodes.get(goto_.getTarget()));
            } else if(instruction instanceof JumpIfInstruction jif) {
                Set<Instruction> targets = successors.get(jif);

                CFGNode ifTrueTarget = labelNodes.get(jif.getTarget());
                Iterator<Instruction> it = targets.iterator();

                Instruction normalSuccessor = it.next();
                CFGNode normalNode = instructionToNode.get(normalSuccessor);
                if(normalNode == null) {
                    //Is found
                }else if(normalNode == ifTrueTarget) {
                    normalSuccessor = it.next();
                }

                CFGJumpIfInstruction jifNode = new CFGJumpIfInstruction(null, labelNodes.get(jif.getTarget()), jif.getCondition());
                newInsn = jifNode;
                jifNode.setUnresolvedNormal(normalSuccessor);
                unresolvedIfs.add(jifNode);
            } else if(instruction instanceof SwitchInstruction switch_) {
                CFGSwitchInstruction newSwitch = new CFGSwitchInstruction(switch_.getValue());
                newSwitch.setDefaultTarget(labelNodes.get(switch_.getDefaultBranch()));

                for (Int2ObjectMap.Entry<Label> entry : switch_.getBranches().int2ObjectEntrySet()) {
                    newSwitch.addTarget(entry.getIntKey(), labelNodes.get(entry.getValue()));
                }
            }

            CFGNode node = new CFGNode(newInsn, nextID());
            instructionToNode.put(instruction, node);
        }

        for (CFGJumpIfInstruction unresolvedIf : unresolvedIfs) {
            unresolvedIf.setNormal(
                    instructionToNode.get(
                            unresolvedIf.getUnresolvedNormal()
                    )
            );
        }

        //Create the edges
        for (Map.Entry<Instruction, Set<Instruction>> entry : successors.entrySet()) {
            CFGNode from = instructionToNode.get(entry.getKey());

            for(Instruction toInsn: entry.getValue()) {
                CFGNode to = instructionToNode.get(toInsn);
                from.addSuccessor(to);
            }
        }

        //Redirect label instructions
        for(CFGNode node: instructionToNode.values()) {
            if(node.getInstruction() instanceof LabelInstruction labelInsn) {
                CFGNode actualTarget = labelNodes.get(labelInsn.getLabel());

                while (actualTarget.getInstruction() instanceof LabelInstruction) {
                    if(actualTarget.getSuccessors().size() != 1) {
                        throw new IllegalStateException("Doesn't have a single successor. (It has " + actualTarget.getSuccessors().size() + ")");
                    }

                    actualTarget = actualTarget.getSuccessors().iterator().next();
                }

                //Preven concurrent modification exception
                Set<CFGNode> predecessorCopy = new HashSet<>(node.getPredecessors());
                for (CFGNode predecessor : predecessorCopy) {
                    predecessor.replaceSuccessor(node, actualTarget);
                }
            }
        }

        //Disconnect all label successors
        for(CFGNode node: instructionToNode.values()) {
            if(node.getInstruction() instanceof LabelInstruction) {
                for(CFGNode successor: node.getSuccessors()) {
                    successor.getPredecessors().remove(node);
                    successor.updateDominators();
                }

                node.getSuccessors().clear();
                node.updateReachable();
            }
        }

        //Add goto between jumpifs and both their targets
        for(CFGNode node: instructionToNode.values()) {
            if(node.getInstruction() instanceof CFGJumpIfInstruction jif) {
                CFGNode normalTarget = jif.getNormal();
                CFGNode trueTarget = jif.getIfTrue();

                CFGGotoInstruction gotoNormal = new CFGGotoInstruction(normalTarget);
                CFGGotoInstruction gotoTrue = new CFGGotoInstruction(trueTarget);

                CFGNode gotoNormalNode = new CFGNode(gotoNormal, nextID());
                CFGNode gotoTrueNode = new CFGNode(gotoTrue, nextID());

                gotoNormalNode.addSuccessor(normalTarget);
                gotoTrueNode.addSuccessor(trueTarget);

                node.replaceSuccessor(normalTarget, gotoNormalNode);
                node.replaceSuccessor(trueTarget, gotoTrueNode);
            }
        }

        //Find start node. It should be the only instruction with no predecessors but with successors. (Label instructions will now have no predecessors or successors)
        List<CFGNode> potentialStartNodes;
        potentialStartNodes = this.instructionToNode.values().stream().filter(n -> n.getPredecessors().size() == 0 && n.getSuccessors().size() > 0).toList();

        if (potentialStartNodes.size() == 0) {
            potentialStartNodes = this.instructionToNode.values().stream().filter(n -> n.getPredecessors().size() == 0 && (n.getInstruction() instanceof ReturnInstruction)).toList();
        }

        if (potentialStartNodes.size() == 0) {
            throw new IllegalStateException("No start node found");
        }else if(potentialStartNodes.size() > 1) {
            throw new IllegalStateException("Multiple start nodes found");
        }

        this.root = potentialStartNodes.get(0);
    }

    public Graph display() {
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("CFG");
        Map<CFGNode, Node> nodes = new HashMap<>();

        for(CFGNode node: Util.makeSet(this)){
            node.updateReachable();
        }

        for(CFGNode node: this) {
            node.checkReachable();
            Node graphNode = graph.addNode(node.nodeName());
            graphNode.setAttribute("ui.label", node.getId());
            nodes.put(node, graphNode);
        }

        for(CFGNode from: this) {
            for(CFGNode to: from.getSuccessors()) {
                graph.addEdge(from.getId() + "->" + to.getId(), nodes.get(from), nodes.get(to), true);
            }
        }

        //Get style
        try {
            InputStream is = ControlFlowGraph.class.getResourceAsStream("/gsstyle.css");
            String style = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            graph.setAttribute("ui.stylesheet", style);
        } catch (IOException e) {
            System.err.println("Couldn't load style");
            e.printStackTrace();
        }

        graph.setAttribute("ui.quality");
        graph.display();

        return graph;
    }

    public SubGraph full() {
        return new SubGraph(this, Util.makeSet(this));
    }

    @Override
    public Iterator<CFGNode> iterator() {
        return new Iterator<>() {
            boolean doneRoot = false;
            final Iterator<CFGNode> others = root.getReachable().iterator();

            @Override
            public boolean hasNext() {
                return !doneRoot || others.hasNext();
            }

            @Override
            public CFGNode next() {
                if(!doneRoot) {
                    doneRoot = true;
                    return root;
                }else {
                    return others.next();
                }
            }
        };
    }

    private int nextID(){
        return idCounter.getAndIncrement();
    }

    public CFGNode getRoot() {
        return root;
    }

    public boolean hasSpecialEdge(CFGNode from, CFGNode to) {
        return specialEdges.containsKey(new Edge(from, to));
    }

    public SpecialEdge getSpecialEdge(CFGNode from, CFGNode to) {
        return specialEdges.get(new Edge(from, to));
    }

    public void putSpecialEdge(CFGNode from, CFGNode to, SpecialEdge edge) {
        Edge key = new Edge(from, to);
        if(specialEdges.containsKey(key)) {
            throw new IllegalStateException("Edge already exists");
        }

        specialEdges.put(key, edge);
    }

    public @Nullable SpecialEdge edgeFrom(CFGNode from) {
        for (Map.Entry<Edge, SpecialEdge> entry : specialEdges.entrySet()) {
            if (entry.getKey().from() == from) {
                return entry.getValue();
            }
        }

        return null;
    }
}
