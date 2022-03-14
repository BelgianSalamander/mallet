package me.salamander.mallet.shaders.compiler.ast.make.set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.ControlFlowGraph;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class StructureEncapsulationTree {
    private final SETNode root;
    final List<SETNode> nodes = new ArrayList<>();
    final Object2IntMap<SETNode> node2id = new Object2IntOpenHashMap<>();

    public StructureEncapsulationTree(ControlFlowGraph cfg) {
        this.root = new WholeMethodNode(this, cfg);

        this.nodes.add(root);
        this.node2id.put(root, 0);
    }

    public void addNode(SETNode node) {
        root.add(node);

        nodes.add(node);
        node2id.put(node, nodes.size() - 1);
    }

    public void print(PrintStream out) {
        root.print(out, "");
    }

    public SETNode getNode(int id) {
        return nodes.get(id);
    }

    public SETNode getRoot() {
        return root;
    }

    public int getId(SETNode node) {
        return node2id.getInt(node);
    }
}
