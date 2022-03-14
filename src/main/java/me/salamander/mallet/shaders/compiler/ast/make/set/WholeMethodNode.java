package me.salamander.mallet.shaders.compiler.ast.make.set;

import me.salamander.mallet.shaders.compiler.ast.node.ASTNode;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.shaders.compiler.ast.node.MethodASTNode;
import me.salamander.mallet.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WholeMethodNode extends SETNode {
    private final CFGNode entryPoint;

    public WholeMethodNode(StructureEncapsulationTree tree, ControlFlowGraph cfg) {
        super(tree, Util.makeSet(cfg), Set.of(Util.makeSet(cfg)));

        this.entryPoint = cfg.getRoot();

        for(CFGNode node : this.body) {
            node.SETParent = this;
        }
    }

    @Override
    public CFGNode getEntryPoint() {
        return entryPoint;
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<SETNode> sort = this.topologicalSort.values().iterator().next();

        List<ASTNode> ast = new ArrayList<>();

        for (SETNode setNode : sort) {
            ast.addAll(setNode.makeAST(cfg));
        }

        return List.of(
                new MethodASTNode(ast)
        );
    }
}
