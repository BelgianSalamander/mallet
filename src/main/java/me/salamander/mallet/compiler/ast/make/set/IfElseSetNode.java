package me.salamander.mallet.compiler.ast.make.set;

import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.node.IfElseASTNode;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IfElseSetNode extends SETNode {
    private final CFGNode head;
    private final Value condition;
    private final Set<CFGNode> ifTrue;
    private final Set<CFGNode> ifFalse;

    public IfElseSetNode(StructureEncapsulationTree tree, CFGNode head, Value condition, Set<CFGNode> ifTrue, Set<CFGNode> ifFalse) {
        super(
                tree,
                Util.union(Set.of(head), ifTrue, ifFalse),
                Set.of(Set.of(head), ifTrue, ifFalse)
        );

        this.head = head;
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    public CFGNode getHead() {
        return head;
    }

    public Value getCondition() {
        return condition;
    }

    public Set<CFGNode> getIfTrue() {
        return ifTrue;
    }

    public Set<CFGNode> getIfFalse() {
        return ifFalse;
    }

    @Override
    public CFGNode getEntryPoint() {
        return head;
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<ASTNode> ifTrueBody = new ArrayList<>();
        List<ASTNode> ifFalseBody = new ArrayList<>();

        for (SETNode setNode : topologicalSort.get(ifTrue)) {
            ifTrueBody.addAll(setNode.makeAST(cfg));
        }

        for (SETNode setNode : topologicalSort.get(ifFalse)) {
            ifFalseBody.addAll(setNode.makeAST(cfg));
        }

        return List.of(
                new IfElseASTNode(
                        ifTrueBody,
                        ifFalseBody,
                        condition
                )
        );
    }
}
