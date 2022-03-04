package me.salamander.mallet.compiler.ast.make.set;

import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.node.LabelledBlockASTNode;
import me.salamander.mallet.compiler.ast.node.LoopASTNode;
import me.salamander.mallet.compiler.instruction.value.LiteralValue;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LabelledBlockSetNode extends SETNode {
    private final List<SETNode> sort;
    private final SETNode exit;

    public LabelledBlockSetNode(StructureEncapsulationTree tree, Set<CFGNode> body, List<SETNode> sort, SETNode exit) {
        super(tree, body, Set.of(body));

        this.sort = sort;
        this.topologicalSort.put(body, sort);
        this.exit = exit;
    }

    @Override
    public CFGNode getEntryPoint() {
        return sort.get(0).getEntryPoint();
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<ASTNode> body = new ArrayList<>();

        for (SETNode setNode : topologicalSort.values().iterator().next()) {
            body.addAll(setNode.makeAST(cfg));
        }

        return List.of(
                new LabelledBlockASTNode(
                        this.getLabel(),
                        body
                )
        );
    }

    public SETNode getExit() {
        return exit;
    }
}
