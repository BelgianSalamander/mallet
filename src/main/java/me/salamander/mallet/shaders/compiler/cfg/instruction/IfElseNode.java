package me.salamander.mallet.shaders.compiler.cfg.instruction;

import me.salamander.mallet.shaders.compiler.cfg.instruction.instructions.CFGGotoInstruction;
import me.salamander.mallet.shaders.compiler.cfg.instruction.instructions.CFGJumpIfInstruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

public class IfElseNode extends NodeWithInner {
    private final InnerCFGNode trueBranch;
    private final InnerCFGNode falseBranch;
    private final Value condition;
    private final InstructionNode head;

    public IfElseNode(int id, InstructionCFG parent, InstructionNode head, CFGNode trueBranch, CFGNode falseBranch) {
        super(id, parent);
        this.head = head;

        CFGJumpIfInstruction jumpIf = (CFGJumpIfInstruction) head.getInstruction();
        this.condition = jumpIf.getCondition();

        trueBranch = actualBranch(trueBranch);
        falseBranch = actualBranch(falseBranch);

        Predicate<CFGNode> canBelongToTrue = makeCanBelong(trueBranch);
        Predicate<CFGNode> canBelongToFalse = makeCanBelong(falseBranch);

        this.trueBranch = makeBody(trueBranch, canBelongToTrue);
        this.falseBranch = makeBody(falseBranch, canBelongToFalse);
    }

    private InnerCFGNode makeBody(CFGNode branchHead, Predicate<CFGNode> condition) {
        Set<CFGNode> bodyNodes = new HashSet<>();

        Stack<CFGNode> toProcess = new Stack<>();
        Set<CFGNode> processed = new HashSet<>();
        toProcess.push(branchHead);

        while (!toProcess.isEmpty()) {
            CFGNode node = toProcess.pop();

            if (!processed.add(node)) {
                continue;
            }

            if(condition.test(node)) {
                bodyNodes.add(node);
                toProcess.addAll(node.successors);
            }
        }

        InnerCFGNode body = this.parent.groupNodes(bodyNodes);
        return body;
    }

    private CFGNode actualBranch(CFGNode branchTarget) {
        if (!branchTarget.isDominatedBy(head)) {
            CFGGotoInstruction gotoInstruction = new CFGGotoInstruction(branchTarget);
            InstructionNode gotoNode = new InstructionNode(gotoInstruction, this.parent.getIdCounter().getAndIncrement(), this.parent);
            gotoNode.addSuccessor(branchTarget);
            head.replaceSuccessor(branchTarget, gotoNode);

            return gotoNode;
        }

        return branchTarget;
    }

    private Predicate<CFGNode> makeCanBelong(CFGNode bodyStart) {
        Predicate<CFGNode> canBelongToBody = (node) -> node.isDominatedBy(bodyStart);

        if (parent.parent != null && parent.parent instanceof LoopNode loopNode) {
            //We don't want to loop around
            CFGNode loopHead = loopNode.getEntryPoint();
            canBelongToBody = canBelongToBody.and((node) -> node != loopHead);
        }

        return canBelongToBody;
    }

    @Override
    public Collection<InnerCFGNode> innerCFGS() {
        return Set.of(trueBranch, falseBranch);
    }

    @Override
    protected String getDescription() {
        return null;
    }

    @Override
    public void printInfo(PrintStream out) {
        System.out.println("IF ELSE Node: " + this.id);
        System.out.println("Condition: " + condition);

        System.out.println("=== True Branch START (" + this.id + ") === (" + trueBranch.id + ")");
        trueBranch.getCFG().printInfo(out);
        System.out.println("=== True Branch END (" + this.id + ") ===");

        System.out.println("=== False Branch START (" + this.id + ") === (" + falseBranch.id + ")");
        falseBranch.getCFG().printInfo(out);
        System.out.println("=== False Branch END (" + this.id + ") ===");

        System.out.println("Successors: ");
        for(CFGNode node : this.getAllSuccessors()) {
            System.out.println("\t" + node.id);
        }
    }

    public void addToCFG() {
        this.parent.addNodeWithCFGs(this, head);

        this.trueBranch.getCFG().detectIfs();
        this.falseBranch.getCFG().detectIfs();
    }
}

