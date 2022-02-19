package me.salamander.mallet.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.compiler.JavaDecompiler;
import me.salamander.mallet.compiler.cfg.BasicBlock;
import me.salamander.mallet.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.compiler.instruction.*;
import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.compiler.instruction.value.VariableType;
import me.salamander.mallet.util.Pair;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InstructionCFG {
    private final JavaDecompiler decompiler;
    private final Map<Instruction, CFGNode> nodes = new IdentityHashMap<>();
    private final CFGNode start;
    private final AtomicInteger idCounter = new AtomicInteger();

    public InstructionCFG(IntermediaryCFG intermediaryCFG, JavaDecompiler decompiler) {
        this.decompiler = decompiler;
        //Create the nodes first

        for (BasicBlock block : intermediaryCFG.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                getNode(instruction);
            }
        }

        //Create the edges
        for (BasicBlock block : intermediaryCFG.getBlocks()) {
            //Create intra-block edges
            for (int i = 0; i < block.getInstructions().size() - 1; i++) {
                Instruction instruction = block.getInstructions().get(i);
                Instruction nextInstruction = block.getInstructions().get(i + 1);

                getNode(instruction).addSuccessor(getNode(nextInstruction));
            }

            if (block.getInstructions().size() == 0) {
                //Is start or end block
                continue;
            }

            //Create inter-block edges
            Instruction lastInstruction = block.getInstructions().get(block.getInstructions().size() - 1);

            for (BasicBlock successor : block.getNext()) {
                if (successor.getInstructions().size() == 0) {
                    //Is start or end block
                    continue;
                }

                Instruction firstInstruction = successor.getInstructions().get(0);

                getNode(lastInstruction).addSuccessor(getNode(firstInstruction));
            }
        }

        //Get start node
        BasicBlock startBlock = intermediaryCFG.getStartBlock();

        if (startBlock.getNext().size() > 1) {
            throw new RuntimeException("Start block has more than one successor");
        } else if (startBlock.getNext().size() == 0) {
            throw new RuntimeException("Start block has no successors");
        }

        start = getNode(startBlock.getNext().get(0).getInstructions().get(0));

        start.visitDepthFirstPreorder(node -> node.printInfo(System.out));
    }

    public void detectLoops() {
        Set<StronglyConnectedComponent> sccs = findStronglyConnectedComponents();

        for (StronglyConnectedComponent scc : sccs) {
            makeLoop(scc);
        }
    }

    private void makeLoop(StronglyConnectedComponent scc) {
        //Find all entry points

        Set<InstructionNode> entryPoints = scc.nodes.stream().filter(node -> !scc.nodes.containsAll(node.predecessors)).collect(Collectors.toSet());
        System.out.println("Found " + entryPoints.size() + " entry points");
        InstructionNode singleEntryPoint;

        if (entryPoints.size() > 1) {
            //Find the entry point with the fewest predecessors
            singleEntryPoint = Collections.min(entryPoints, Comparator.comparingInt(node -> node.predecessors.size()));
            singleEntryPoint = makeSingleEntryPoint(singleEntryPoint, entryPoints, scc);
        } else {
            singleEntryPoint = entryPoints.iterator().next();
        }
    }

    private InstructionNode makeSingleEntryPoint(InstructionNode naturalEntryPoint, Set<InstructionNode> entryPoints, StronglyConnectedComponent scc) {
        entryPoints.remove(naturalEntryPoint);

        Map<InstructionNode, Integer> entryPointFlags = new IdentityHashMap<>();
        List<InstructionNode> entryPointList = new ArrayList<>();
        entryPointFlags.put(naturalEntryPoint, 0);
        entryPointList.add(naturalEntryPoint);

        int flag = 1;
        for (InstructionNode entryPoint : entryPoints) {
            entryPointFlags.put(entryPoint, flag++);
            entryPointList.add(entryPoint);
        }

        Variable flagVar = new Variable(Type.INT_TYPE, decompiler.getNextTempVar(), VariableType.SYNTHETIC);

        //Make dispatch statement
        Label switchLabel = decompiler.makeLabel();
        InstructionNode switchLabelNode = getNode(new LabelInstruction(switchLabel));

        SwitchInstruction switchInstruction = new SwitchInstruction(flagVar);
        InstructionNode switchNode = getNode(switchInstruction);

        for (int i = 0; i < entryPointList.size(); i++) {
            InstructionNode target = entryPointList.get(i);
            var actualTarget = getLabel(target);
            switchInstruction.addBranch(i, actualTarget.right());
            switchNode.addSuccessor(actualTarget.left());
        }

        //Redirect things to switch node
        for (InstructionNode predecessor : naturalEntryPoint.predecessors) {
            redirect(predecessor, naturalEntryPoint, switchLabelNode, switchLabel);
        }

        for (InstructionNode entryPoint : entryPoints) {
            for (InstructionNode predecessor : entryPoint.predecessors) {
                if (!scc.nodes.contains(predecessor)) {
                    redirect(predecessor, entryPoint, switchLabelNode, switchLabel);
                }
            }
        }

        return switchLabelNode;
    }

    private InstructionNode getNode(Instruction instruction) {
        return nodes.computeIfAbsent(instruction, (key) -> new InstructionNode(instruction, idCounter.getAndIncrement()));
    }

    private InstructionNode redirect(InstructionNode from, InstructionNode normalSuccessor, InstructionNode labelNode, Label label) {
        if (normalSuccessor.instruction instanceof LabelInstruction labelInstruction) {
            if (from.instruction instanceof GotoInstruction goto_) {
                goto_.setTarget(label);
            } else if (from.instruction instanceof JumpIfInstruction jumpIf) {
                if (jumpIf.getTarget() == labelInstruction.getLabel()) {
                    jumpIf.setTarget(label);
                }
            } else if (from.instruction instanceof SwitchInstruction switch_) {
                if (switch_.getDefaultBranch() == labelInstruction.getLabel()) {
                    switch_.setDefaultBranch(label);
                }

                IntSet change = switch_.getBranches().int2ObjectEntrySet().stream().filter(entry -> entry.getValue() == labelInstruction.getLabel()).mapToInt(Map.Entry::getKey).collect(IntOpenHashSet::new, IntSet::add, IntSet::addAll);

                for (int i : change) {
                    switch_.getBranches().put(i, label);
                }
            }
        }

        from.replaceSuccessor(normalSuccessor, labelNode);
    }

    private Pair<InstructionNode, Label> getLabel(InstructionNode target) {
        if (target.instruction instanceof LabelInstruction labelInstruction) {
            return new Pair<>(target, labelInstruction.getLabel());
        } else {
            Label label = decompiler.makeLabel();
            LabelInstruction labelInstruction = new LabelInstruction(label);
            InstructionNode node = getNode(labelInstruction);

            for (InstructionNode predecessor : target.predecessors) {
                //If it was a control flow statement it would have to point to a label so we don't need to tamper with the actual instructions
                predecessor.addSuccessor(node);
                predecessor.removeSuccessor(target);
            }

            node.addSuccessor(target);

            return new Pair<>(node, label);
        }
    }

    private Set<StronglyConnectedComponent> findStronglyConnectedComponents() {
        //Kosaraju's algorithm https://en.wikipedia.org/wiki/Kosaraju%27s_algorithm

        //Step 1
        boolean[] visited = new boolean[nodes.size()];
        List<InstructionNode> order = new ArrayList<>(nodes.size());

        //Step 2:
        /*for(Node node: nodes.values()) {
            if(!visited[node.id]) {
                kosarajuVisit(node, visited, order);
            }
        }*/
        start.visitDepthFirstPostorder(order::add);
        Collections.reverse(order);

        //Step 3:
        StronglyConnectedComponent[] components = new StronglyConnectedComponent[nodes.size()];
        for (InstructionNode node : order) {
            kosarajuAssign(node, node, components);
        }

        Set<StronglyConnectedComponent> sccs = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        for (StronglyConnectedComponent component : components) {
            if (component != null && component.nodes.size() > 1) {
                sccs.add(component);
            }
        }

        return sccs;
    }

    private void kosarajuAssign(InstructionNode node, InstructionNode root, StronglyConnectedComponent[] components) {
        if (components[node.id] != null) {
            return;
        }

        components[node.id] = components[root.id] == null ? new StronglyConnectedComponent() : components[root.id];
        components[node.id].nodes.add(node);

        for (InstructionNode predecessor : node.predecessors) {
            kosarajuAssign(predecessor, root, components);
        }
    }

    private void kosarajuVisit(InstructionNode node, boolean[] visited, List<InstructionNode> order) {
        if (visited[node.id]) {
            return;
        }

        visited[node.id] = true;

        for (InstructionNode successor : node.successors) {
            kosarajuVisit(successor, visited, order);
        }

        order.add(0, node);
    }

    private static record StronglyConnectedComponent(Set<InstructionNode> nodes) {
        public StronglyConnectedComponent() {
            this(new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY));
        }
    }

    private static record Loop(InstructionNode entryPoint, Set<InstructionNode> exitPoint, InstructionCFG body) {

    }
}
