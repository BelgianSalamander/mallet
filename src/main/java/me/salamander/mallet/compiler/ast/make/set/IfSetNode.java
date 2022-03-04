package me.salamander.mallet.compiler.ast.make.set;

import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.node.IfASTNode;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IfSetNode extends SETNode {
    private final CFGNode head;
    private final Value condition;
    private final Set<CFGNode> body;

    public IfSetNode(StructureEncapsulationTree tree, CFGNode head, Value condition, Set<CFGNode> body) {
        super(tree, Util.union(body, Set.of(head)), Set.of(body, Set.of(head)));

        this.head = head;
        this.condition = condition;
        this.body = body;
    }

    @Override
    public CFGNode getEntryPoint() {
        return head;
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<ASTNode> body = new ArrayList<>();

        for (SETNode setNode : this.topologicalSort.get(body)) {
            body.addAll(setNode.makeAST(cfg));
        }

        return List.of(new IfASTNode(body, condition));
    }
}
