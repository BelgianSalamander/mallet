package me.salamander.mallet.compiler.ast.make.set;

import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.compiler.ast.make.cfg.SubGraph;
import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.node.LoopASTNode;
import me.salamander.mallet.compiler.instruction.value.LiteralValue;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoopSetNode extends SETNode {
    private final CFGNode entryPoint;

    public LoopSetNode(StructureEncapsulationTree tree, Set<CFGNode> body, CFGNode entryPoint) {
        super(tree, body, Set.of(body));

        List<CFGNode> potentialEntryPoints = body.stream().filter(node -> !body.containsAll(node.getPredecessors())).toList();

        if(potentialEntryPoints.size() != 1) {
            throw new IllegalArgumentException("LoopSetNode must have exactly one entry point. (Found " + potentialEntryPoints.size() + ")");
        }

        this.entryPoint = entryPoint;
    }

    @Override
    public CFGNode getEntryPoint() {
        return entryPoint;
    }

    @Override
    protected boolean excludeEdgeFromTopologicalSort(SETNode fromSETNode, CFGNode fromCFGNode, SETNode toSETNode, CFGNode toCFGNode) {
        return toCFGNode == entryPoint;
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<ASTNode> body = new ArrayList<>();

        for (SETNode setNode : topologicalSort.values().iterator().next()) {
            body.addAll(setNode.makeAST(cfg));
        }

        return List.of(
                new LoopASTNode(
                        this.getLabel(),
                        body,
                        new LiteralValue(Type.BOOLEAN_TYPE, true)
                )
        );
    }
}
