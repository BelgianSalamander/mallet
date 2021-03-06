package me.salamander.mallet.shaders.compiler.cfg.instruction;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.shaders.compiler.JavaDecompiler;
import me.salamander.mallet.shaders.compiler.cfg.BasicBlock;
import me.salamander.mallet.shaders.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.shaders.compiler.cfg.instruction.instructions.CFGJumpIfInstruction;
import me.salamander.mallet.shaders.compiler.cfg.instruction.instructions.CFGSwitchInstruction;
import me.salamander.mallet.shaders.compiler.instruction.*;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import me.salamander.mallet.shaders.compiler.instruction.value.VariableType;
import me.salamander.mallet.util.Pair;
import me.salamander.mallet.util.Util;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.objectweb.asm.Type;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InstructionCFG {
    private final JavaDecompiler decompiler;
    private final Map<Instruction, CFGNode> nodes = new IdentityHashMap<>();
    private final List<CFGNode> allNodes = new ArrayList<>();
    private final Map<CFGNode, Set<CFGNode>> externalConnections = new IdentityHashMap<>();
    private final StartNode start;
    public NodeWithInner parent;
    private AtomicInteger idCounter = new AtomicInteger();

    public InstructionCFG(IntermediaryCFG intermediaryCFG, JavaDecompiler decompiler) {
        this.decompiler = decompiler;
        //Create the nodes first
        Map<Label, CFGNode> labelNodes = new HashMap<>();

        for (BasicBlock block : intermediaryCFG.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                getNode(instruction);

                if (instruction instanceof LabelInstruction labelInsn) {
                    labelNodes.put(labelInsn.getLabel(), getNode(instruction));
                }
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

        CFGNode start = getNode(startBlock.getNext().get(0).getInstructions().get(0));

        Variable redundantConditions = new Variable(Type.BOOLEAN_TYPE, decompiler.getNextTempVar(), VariableType.SYNTHETIC);

        //Replace control flow instruction with special instructions
        for (CFGNode node : nodes.values()) {
            InstructionNode instructionNode = (InstructionNode) node;

            if (instructionNode.instruction instanceof GotoInstruction gotoInstruction) {
                CFGNode targetLabel = labelNodes.get(gotoInstruction.getTarget());

                for(CFGNode predecessor: node.predecessors) {
                    predecessor.replaceSuccessor(node, targetLabel);
                }
            }else if(instructionNode.instruction instanceof JumpIfInstruction jumpIfInstruction) {
                CFGNode jumpIfTarget = labelNodes.get(jumpIfInstruction.getTarget());
                CFGNode normalFlowLabel = node.successors.stream().filter(s -> !s.equals(jumpIfTarget)).findFirst().orElse(null);

                if(jumpIfTarget == normalFlowLabel) {
                    instructionNode.instruction = new AssignmentInstruction(
                            redundantConditions,
                            jumpIfInstruction.getCondition()
                    );
                }else {
                    instructionNode.instruction = new CFGJumpIfInstruction(normalFlowLabel, jumpIfTarget, jumpIfInstruction.getCondition());
                }
            }else if(instructionNode.instruction instanceof SwitchInstruction switchInstruction) {
                CFGNode defaultLabel = switchInstruction.getDefaultBranch() != null ? labelNodes.get(switchInstruction.getDefaultBranch()) : null;

                CFGSwitchInstruction switchInsn = new CFGSwitchInstruction(switchInstruction.getValue());

                if(defaultLabel != null) {
                    switchInsn.setDefaultTarget(defaultLabel);
                }

                for (Int2ObjectMap.Entry<Label> entry : switchInstruction.getBranches().int2ObjectEntrySet()) {
                    switchInsn.addTarget(entry.getIntKey(), labelNodes.get(entry.getValue()));
                }

                instructionNode.instruction = switchInsn;
            }
        }

        //Remdirect label instructions
        for (CFGNode node : nodes.values()) {
            if (((InstructionNode) node).instruction instanceof LabelInstruction labelInsn) {
                CFGNode actualNode = node;

                while (((InstructionNode) actualNode).instruction instanceof LabelInstruction) {
                    actualNode = actualNode.successors.iterator().next();
                }

                for(CFGNode predecessor: node.predecessors) {
                    predecessor.replaceSuccessor(node, actualNode);
                }
            }
        }

        //Get actual start
        while (((InstructionNode) start).instruction instanceof GotoInstruction || ((InstructionNode) start).instruction instanceof LabelInstruction) {
            start = start.successors.iterator().next();
        }

        //Remove any floating labels and gotos
        for (CFGNode node : allNodes) {
            if (node instanceof InstructionNode) {
                Instruction instruction = ((InstructionNode) node).instruction;

                if (instruction instanceof GotoInstruction || instruction instanceof LabelInstruction) {
                    InstructionNode insnNode = (InstructionNode) node;

                    while(!node.successors.isEmpty()) {
                        CFGNode successor = node.successors.iterator().next();
                        insnNode.removeSuccessor(successor);
                    }
                }
            }
        }

        this.start = new StartNode(idCounter.getAndIncrement(), this, start);
        allNodes.add(this.start);
    }

    private InstructionCFG(CFGNode entryPoint, Set<CFGNode> bodyNodes, AtomicInteger idCounter, JavaDecompiler javaDecompiler, Map<CFGNode, Set<CFGNode>> externalConnections) {
        this.decompiler = javaDecompiler;
        this.start = new StartNode(idCounter.getAndIncrement(), this, entryPoint);
        this.allNodes.addAll(bodyNodes);
        this.allNodes.add(this.start);
        this.idCounter = idCounter;
        this.externalConnections.putAll(externalConnections);

        bodyNodes.forEach(node -> node.parent = this);
    }

    public void detectLoops() {
        detectLoops(null);
    }

    public void detectIfs() {
        boolean changed = true;
        boolean triggeredInners = false;

        while (changed) {
            changed = false;

            Stack<CFGNode> toProcess = new Stack<>();
            Set<CFGNode> processed = new HashSet<>();
            toProcess.push(start);
            while (!toProcess.isEmpty()) {
                CFGNode node = toProcess.pop();

                if(!processed.add(node)) {
                    continue;
                }

                if(node instanceof NodeWithInner nodeWithInner && !triggeredInners) {
                    for(InnerCFGNode inner: nodeWithInner.innerCFGS()) {
                        inner.getCFG().detectIfs();
                    }
                }

                if(node instanceof InstructionNode insnNode) {
                    if(insnNode.instruction instanceof CFGJumpIfInstruction jumpIf) {
                        if(jumpIf.getIfTrue().canReach(jumpIf.getNormal(), node1 -> node1 != node, false)) {
                            IfNode ifNode = new IfNode(idCounter.getAndIncrement(), this, insnNode, jumpIf.getIfTrue(), jumpIf.getNormal(), false);
                            ifNode.addToCFG();
                            changed = true;
                            break;
                        }else if(jumpIf.getNormal().canReach(jumpIf.getIfTrue(), node1 -> node1 != node, false)) {
                            IfNode ifNode = new IfNode(idCounter.getAndIncrement(), this, insnNode, jumpIf.getNormal(), jumpIf.getIfTrue(), true);
                            ifNode.addToCFG();
                            changed = true;
                            break;
                        }else{
                            IfElseNode ifElseNode = new IfElseNode(idCounter.getAndIncrement(), this, insnNode, jumpIf.getIfTrue(), jumpIf.getNormal());
                            ifElseNode.addToCFG();
                            changed = true;
                            break;
                        }
                    }
                }

                toProcess.addAll(node.successors);
            }

            triggeredInners = true;
        }
    }

    public InnerCFGNode groupNodes(Set<CFGNode> bodyNodes) {
        Set<CFGNode> entryPoints = bodyNodes.stream().filter(node -> !bodyNodes.containsAll(node.predecessors)).collect(Collectors.toSet());
        if(entryPoints.size() != 1) {
            throw new RuntimeException("Entry point not found");
        }

        CFGNode entryPoint = entryPoints.iterator().next();

        Set<CFGNode> exitPoints = bodyNodes.stream().filter(node -> !bodyNodes.containsAll(node.getAllSuccessors())).collect(Collectors.toSet());

        Map<CFGNode, Set<CFGNode>> exits = new IdentityHashMap<>();
        Set<CFGNode> externalExits = new HashSet<>();

        for(CFGNode exitPoint: exitPoints){
            Set<CFGNode> exitNodes = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
            for(CFGNode node: exitPoint.getAllSuccessors()){
                if(!bodyNodes.contains(node)){
                    exitNodes.add(node);

                    if(!exitPoint.successors.contains(node)){
                        externalExits.add(node);
                    }
                }
            }
            exits.put(exitPoint, exitNodes);
            exitPoint.successors.removeIf(node -> !bodyNodes.contains(node));
        }

        Set<CFGNode> inNeighbors = entryPoint.predecessors.stream().filter(node -> !bodyNodes.contains(node)).collect(Collectors.toSet());
        entryPoint.predecessors.removeIf(node -> !bodyNodes.contains(node));

        InstructionCFG newCFG = new InstructionCFG(entryPoint, bodyNodes, idCounter, decompiler, exits);
        InnerCFGNode innerCFG = new InnerCFGNode(idCounter.getAndIncrement(), this, newCFG);
        this.allNodes.add(innerCFG);

        for(CFGNode inNeighbor: inNeighbors){
            inNeighbor.replaceSuccessor(entryPoint, innerCFG);
        }

        innerCFG.predecessors.addAll(inNeighbors);

        for(Set<CFGNode> exit: exits.values()){
            //innerCFG.successors.addAll(exit);
            for(CFGNode exitNode: exit){
                if(!externalExits.contains(exitNode)) {
                    innerCFG.successors.add(exitNode);
                }
            }
        }

        for(CFGNode exitPoint: exitPoints){
            if(this.externalConnections.containsKey(exitPoint)){
                this.externalConnections.computeIfAbsent(
                        innerCFG,
                        k -> new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY)
                ).addAll(this.externalConnections.get(exitPoint));
                this.externalConnections.remove(exitPoint);
            }
        }

        return innerCFG;
    }

    public void addNodeWithCFGs(NodeWithInner node, CFGNode entryPoint){
        Set<CFGNode> inNeighbors = entryPoint.predecessors;

        node.predecessors.clear();
        node.predecessors.addAll(inNeighbors);

        for(CFGNode inNeighbor: inNeighbors) {
            inNeighbor.replaceSuccessor(entryPoint, node);
        }

        Set<CFGNode> successors = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);
        successors.addAll(entryPoint.successors);
        for(InnerCFGNode innerCFG: node.innerCFGS()){
            successors.addAll(innerCFG.successors);
            innerCFG.getCFG().parent = node;
        }
        successors.removeIf(node1 -> node.innerCFGS().contains(node1));
        node.successors.clear();
        node.successors.addAll(successors);

        for(CFGNode successor: successors){
            successor.predecessors.add(node);
            successor.predecessors.remove(entryPoint);
            successor.predecessors.removeAll(node.innerCFGS());
            successor.predecessors.removeIf(n -> {
                for(InnerCFGNode innerCFG: node.innerCFGS()){
                    if(innerCFG.getCFG().deepContains(n)){
                        return true;
                    }
                }
                return false;
            });
        }

        entryPoint.predecessors.clear();
        entryPoint.successors.clear();
    }

    public void displayGraph(String name) {
        System.setProperty("org.graphstream.ui", "swing");
        Graph graph = new SingleGraph("CFG-" + name);

        graph.setStrict(false);
        graph.setAutoCreate(true);

        Map<CFGNode, Node> graphNodes = new Object2ObjectOpenHashMap<>();
        Function<CFGNode, Node> nodeMaker = key -> graph.addNode("Node #" + ((CFGNode) key).id);
        this.start.visitDepthFirstPreorder(
                node -> {},
                (from, to) -> {
                    graph.addEdge(
                            from.id + " -> " + to.id,
                            graphNodes.computeIfAbsent(from, nodeMaker),
                            graphNodes.computeIfAbsent(to, nodeMaker),
                            true
                    );
                }
        );

        Viewer viewer = graph.display();
    }

    /*private void makeIf(CFGNode headNode, CFGJumpIfInstruction jumpIf, CFGNode branchSuccessor, CFGNode end, boolean invertCondition) {
        System.out.println("Made IF");
        Value condition = jumpIf.getCondition();

        if(invertCondition) {
            condition = new UnaryOperation(condition, UnaryOperation.Op.NOT);
        }

        Set<CFGNode> bodySet = new HashSet<>();

        for(CFGNode potentialBody: headNode.reachable) {
            if(branchSuccessor.dominators.contains(potentialBody)){
                bodySet.add(potentialBody);
            }
        }

        new IfNode(headNode, branchSuccessor, bodySet, this, decompiler, idCounter.getAndIncrement());
    }*/

    public void detectLoops(CFGNode ignore) {
        Set<StronglyConnectedComponent> sccs = findStronglyConnectedComponents(ignore);

        System.out.println("Found " + sccs.size() + " SCCs");

        for (StronglyConnectedComponent scc : sccs) {
            makeLoop(scc);
        }
    }

    public void printInfo(PrintStream out){
        this.start.visitDepthFirstPreorder(node -> node.printInfo(out));
    }

    private void makeLoop(StronglyConnectedComponent scc) {
        //Find all entry points
        System.out.println("Making loop for SCC: " + scc);

        Set<CFGNode> entryPoints = scc.nodes.stream().filter(node -> !scc.nodes.containsAll(node.predecessors)).collect(Collectors.toSet());

        System.out.println("Found " + entryPoints.size() + " entry points");
        CFGNode singleEntryPoint;

        if (entryPoints.size() > 1) {
            //Find the entry point with the fewest predecessors
            singleEntryPoint = Collections.min(entryPoints, Comparator.comparingInt(node -> node.predecessors.size()));
            singleEntryPoint = makeSingleEntryPoint(singleEntryPoint, entryPoints, scc);
        } else {
            singleEntryPoint = entryPoints.iterator().next();
        }

        LoopNode loopNode = new LoopNode(singleEntryPoint, scc.nodes, this, idCounter.getAndIncrement());
        this.addNodeWithCFGs(loopNode, loopNode.getBody());
    }

    private CFGNode makeSingleEntryPoint(CFGNode naturalEntryPoint, Set<CFGNode> entryPoints, StronglyConnectedComponent scc) {
        entryPoints.remove(naturalEntryPoint);

        Map<CFGNode, Integer> entryPointFlags = new IdentityHashMap<>();
        List<CFGNode> entryPointList = new ArrayList<>();
        entryPointFlags.put(naturalEntryPoint, 0);
        entryPointList.add(naturalEntryPoint);

        int flag = 1;
        for (CFGNode entryPoint : entryPoints) {
            entryPointFlags.put(entryPoint, flag++);
            entryPointList.add(entryPoint);
        }

        Variable flagVar = new Variable(Type.INT_TYPE, decompiler.getNextTempVar(), VariableType.SYNTHETIC);

        //Make dispatch statement
        Label switchLabel = decompiler.makeLabel();
        CFGNode switchLabelNode = getNode(new LabelInstruction(switchLabel));

        SwitchInstruction switchInstruction = new SwitchInstruction(flagVar);
        CFGNode switchNode = getNode(switchInstruction);

        for (int i = 0; i < entryPointList.size(); i++) {
            CFGNode target = entryPointList.get(i);
            var actualTarget = getLabel((InstructionNode) target);
            switchInstruction.addBranch(i, actualTarget.right());
            switchNode.addSuccessor(actualTarget.left());
        }

        //Redirect things to switch node
        for (CFGNode predecessor : naturalEntryPoint.predecessors) {
            predecessor.replaceSuccessor(naturalEntryPoint, switchNode);
        }

        for (CFGNode entryPoint : entryPoints) {
            for (CFGNode predecessor : entryPoint.predecessors) {
                if (!scc.nodes.contains(predecessor)) {
                    predecessor.replaceSuccessor(entryPoint, switchNode);
                }
            }
        }

        return switchLabelNode;
    }

    private InstructionNode getNode(Instruction instruction) {
        InstructionNode node = (InstructionNode) nodes.computeIfAbsent(instruction, (key) -> new InstructionNode(instruction, idCounter.getAndIncrement(), this));
        allNodes.add(node);
        return node;
    }

    private Pair<InstructionNode, Label> getLabel(InstructionNode target) {
        if (target.getInstruction() instanceof LabelInstruction labelInstruction) {
            return new Pair<>(target, labelInstruction.getLabel());
        } else {
            Label label = decompiler.makeLabel();
            LabelInstruction labelInstruction = new LabelInstruction(label);
            InstructionNode node = getNode(labelInstruction);

            for (CFGNode predecessor : target.predecessors) {
                //If it was a control flow statement it would have to point to a label so we don't need to tamper with the actual instructions
                predecessor.replaceSuccessor(target, node);
            }

            node.addSuccessor(target);

            return new Pair<>(node, label);
        }
    }

    private Set<StronglyConnectedComponent> findStronglyConnectedComponents(CFGNode ignore) {
        //Kosaraju's algorithm https://en.wikipedia.org/wiki/Kosaraju%27s_algorithm
        int size = idCounter.get();

        //Step 1
        boolean[] visited = new boolean[size];
        List<CFGNode> order = new ArrayList<>(size);

        //Step 2:
        for(CFGNode node: allNodes) {
            if(!visited[node.id]) {
                kosarajuVisit(node, visited, order, ignore);
            }
        }

        //Step 3:
        StronglyConnectedComponent[] components = new StronglyConnectedComponent[size];
        for (CFGNode node : order) {
            kosarajuAssign(node, node, components, ignore);
        }

        Set<StronglyConnectedComponent> sccs = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        for (StronglyConnectedComponent component : components) {
            if (component != null && component.nodes.size() > 1) {
                sccs.add(component);
            }
        }

        return sccs;
    }

    private void kosarajuVisit(CFGNode node, boolean[] visited, List<CFGNode> order, CFGNode ignore) {
        if(!allNodes.contains(node) || node == ignore) return;

        if(!visited[node.id]) {
            visited[node.id] = true;
            for(CFGNode successor: node.successors) {
                kosarajuVisit(successor, visited, order, ignore);
            }
            order.add(0, node);
        }
    }

    private void kosarajuAssign(CFGNode node, CFGNode root, StronglyConnectedComponent[] components, CFGNode ignore) {
        if(!allNodes.contains(node) || node == ignore) return;

        if (components[node.id] != null) {
            return;
        }

        components[node.id] = components[root.id] == null ? new StronglyConnectedComponent() : components[root.id];
        components[node.id].nodes.add(node);

        for (CFGNode predecessor : node.predecessors) {
            kosarajuAssign(predecessor, root, components, ignore);
        }
    }

    public void addNode(CFGNode node) {
        this.allNodes.add(node);
    }

    public List<CFGNode> getNodes() {
        return allNodes;
    }

    public boolean deepContains(CFGNode node) {
        Stack<CFGNode> stack = new Stack<>();
        Set<CFGNode> visited = new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY);

        stack.push(this.getHead());

        while(!stack.isEmpty()) {
            CFGNode n = stack.pop();

            if(!visited.add(n)) continue;

            if(n == node) return true;

            if(n instanceof NodeWithInner nodeWithInner){
                for(InnerCFGNode inner: nodeWithInner.innerCFGS()) {
                    if(inner == node) return true;
                    if(inner.getCFG().deepContains(node)) return true;
                }
            }

            for(CFGNode successor: n.successors) {
                stack.push(successor);
            }
        }

        return false;
    }

    public AtomicInteger getIdCounter() {
        return idCounter;
    }

    public StartNode getHead() {
        return start;
    }


    private static record StronglyConnectedComponent(Set<CFGNode> nodes) {
        public StronglyConnectedComponent() {
            this(new ObjectOpenCustomHashSet<>(Util.IDENTITY_HASH_STRATEGY));
        }

        @Override
        public String toString() {
            return "[ " + nodes.stream().map(n -> Integer.toString(n.id)).collect(Collectors.joining(" ")) + " ]";
        }
    }

    public Map<CFGNode, Set<CFGNode>> getExternalConnections() {
        return externalConnections;
    }
}
