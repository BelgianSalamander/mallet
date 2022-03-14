package me.salamander.mallet.shaders.compiler.ast.make.cfg;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import java.util.*;

public class SubGraph {
    private final ControlFlowGraph graph;
    private final Set<CFGNode> members;

    public SubGraph(ControlFlowGraph graph, Set<CFGNode> members) {
        this.graph = graph;
        this.members = members;
    }

    public boolean contains(CFGNode node) {
        return members.contains(node);
    }

    public SubGraph without(CFGNode exclude) {
        Set<CFGNode> newMembers = new HashSet<>(members);
        newMembers.remove(exclude);
        return new SubGraph(graph, newMembers);
    }

    public Set<SubGraph> findStronglyConnectedComponents() {
        //Kosaraju's algorithm https://en.wikipedia.org/wiki/Kosaraju%27s_algorithm
        int size = members.size();

        Object2BooleanMap<CFGNode> visited = new Object2BooleanOpenHashMap<>(size);
        List<CFGNode> order = new ArrayList<>();

        for (CFGNode node: members) {
            if(!visited.getBoolean(node)) {
                kosarajuVisit(node, visited, order);
            }
        }

        Map<CFGNode, Set<CFGNode>> sccs = new HashMap<>(size);
        for(CFGNode node: order) {
            kosarajuAssign(node, node, sccs);
        }

        Set<SubGraph> result = new HashSet<>();
        Set<Set<CFGNode>> visitedSCCS = new HashSet<>();

        for(Set<CFGNode> scc: sccs.values()) {
            if (!visitedSCCS.add(scc)) continue;

            if(scc.size() > 1) {
                result.add(new SubGraph(graph, scc));
                continue;
            }

            //If the node points to itself it counts as an scc
            CFGNode node = scc.iterator().next();

            if(node.getSuccessors().contains(node)) {
                result.add(new SubGraph(graph, scc));
            }
        }

        return result;
    }

    private void kosarajuVisit(CFGNode node, Object2BooleanMap<CFGNode> visited, List<CFGNode> order) {
        if(!visited.getBoolean(node)) {
            visited.put(node, true);
            for (CFGNode succ: node.getSuccessors()) {
                if(members.contains(succ)) {
                    kosarajuVisit(succ, visited, order);
                }
            }
            order.add(0, node);
        }
    }

    private void kosarajuAssign(CFGNode node, CFGNode root, Map<CFGNode, Set<CFGNode>> sccs) {
        if (sccs.get(node) != null) {
            return;
        }

        Set<CFGNode> scc = sccs.computeIfAbsent(root, k -> new HashSet<>());
        scc.add(node);
        sccs.put(node, scc);

        for (CFGNode pred: node.getPredecessors()) {
            if(members.contains(pred)) {
                kosarajuAssign(pred, root, sccs);
            }
        }
    }

    public ControlFlowGraph getGraph() {
        return graph;
    }

    public Set<CFGNode> getMembers() {
        return members;
    }
}
