package me.salamander.mallet.compiler.cfg.instruction;

import me.salamander.mallet.compiler.JavaDecompiler;
import me.salamander.mallet.compiler.cfg.instruction.instructions.CFGGotoInstruction;
import me.salamander.mallet.compiler.cfg.instruction.instructions.CFGJumpIfInstruction;
import me.salamander.mallet.compiler.instruction.value.UnaryOperation;
import me.salamander.mallet.compiler.instruction.value.Value;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Predicate;

public class IfNode extends NodeWithInner {
    private final InnerCFGNode body;
    private final Value condition;
    private final InstructionNode head;
    private final CFGNode join;

    protected IfNode(int id, InstructionCFG parent, InstructionNode head, CFGNode bodyStart, CFGNode join, boolean invertCondition) {
        super(id, parent);
        this.join = join;
        this.head = head;

        CFGJumpIfInstruction jumpIf = (CFGJumpIfInstruction) head.getInstruction();

        if (invertCondition) {
            this.condition = new UnaryOperation(jumpIf.getCondition(), UnaryOperation.Op.NOT);
        } else {
            this.condition = jumpIf.getCondition();
        }

        Predicate<CFGNode> canBelongToBody = makeCanBelong(bodyStart, join);

        if (!makeCanBelong(head, join).test(bodyStart)) {
            //Body would be empty, so we create a body which just has a goto. Below is an example of when this could happen
            /*
             * Source:
             * while(...) {
             *    if(...) { //This "if then break" gets compiled into a conditional jump that exits the loop
             *       break;
             *    }
             * }
             * ...
             */

            CFGGotoInstruction gotoInstruction = new CFGGotoInstruction(bodyStart);
            InstructionNode gotoNode = new InstructionNode(gotoInstruction, this.parent.getIdCounter().getAndIncrement(), this.parent);
            gotoNode.addSuccessor(bodyStart);
            head.replaceSuccessor(bodyStart, gotoNode);

            bodyStart = gotoNode;

            canBelongToBody = makeCanBelong(bodyStart, join);
        }

        Set<CFGNode> bodyNodes = new HashSet<>();

        Stack<CFGNode> toProcess = new Stack<>();
        Set<CFGNode> processed = new HashSet<>();
        toProcess.push(bodyStart);

        while (!toProcess.isEmpty()) {
            CFGNode node = toProcess.pop();

            if (!processed.add(node)) {
                continue;
            }

            if (canBelongToBody.test(node)) {
                bodyNodes.add(node);
                toProcess.addAll(node.successors);
            }
        }

        this.body = parent.groupNodes(bodyNodes);
    }

    private Predicate<CFGNode> makeCanBelong(CFGNode bodyStart, CFGNode join) {
        Predicate<CFGNode> canBelongToBody = (node) -> node.isDominatedBy(bodyStart) && bodyStart.canReach(node, node1 -> node1 != join, true);

        if (parent.parent != null && parent.parent instanceof LoopNode loopNode) {
            //We don't want to loop around
            CFGNode loopHead = loopNode.getEntryPoint();
            canBelongToBody = canBelongToBody.and((node) -> node != loopHead);
        }

        return canBelongToBody;
    }

    public void addToCFG(){
        this.parent.addNodeWithCFGs(this, head);
        this.body.getCFG().detectIfs();
    }

    @Override
    protected String getDescription() {
        return null;
    }

    @Override
    public void printInfo(PrintStream out) {
        System.out.println("IF Node: " + this.id);
        System.out.println("Condition: " + this.condition);
        System.out.println("=== BODY START ===");
        this.body.getCFG().printInfo(out);
        System.out.println("=== BODY END (" + this.id + ") ===");

        System.out.println("Successors:");
        for(CFGNode successor: getAllSuccessors()) {
            System.out.println("\t" + successor.id + (!this.successors.contains(successor) ? " (EXTERNAL)" : ""));
        }
    }

    @Override
    public Collection<InnerCFGNode> innerCFGS() {
        return List.of(this.body);
    }
}
