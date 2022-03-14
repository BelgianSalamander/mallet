package me.salamander.mallet.shaders.compiler.ast.make.set;

import me.salamander.mallet.shaders.compiler.ast.make.cfg.instructions.CFGGotoInstruction;
import me.salamander.mallet.shaders.compiler.ast.node.ASTNode;
import me.salamander.mallet.shaders.compiler.ast.node.InstructionASTNode;
import me.salamander.mallet.shaders.compiler.ast.node.ReturnASTNode;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.shaders.compiler.ast.make.cfg.SpecialEdge;
import me.salamander.mallet.shaders.compiler.instruction.ReturnInstruction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatementSequenceSETNode extends SETNode {
    private final List<CFGNode> sequence;

    public StatementSequenceSETNode(StructureEncapsulationTree tree, List<CFGNode> sequence) {
        super(tree, new HashSet<>(sequence), Set.of(new HashSet<>(sequence)));
        this.sequence = sequence;
    }

    @Override
    public CFGNode getEntryPoint() {
        return sequence.get(0);
    }

    @Override
    public List<ASTNode> makeAST(ControlFlowGraph cfg) {
        List<ASTNode> result = new ArrayList<>();

        for (int i = 0; i < sequence.size(); i++) {
            CFGNode node = sequence.get(i);

            if (node.getInstruction() instanceof CFGGotoInstruction goto_) {
                if (i != sequence.size() - 1) {
                    if (goto_.getTarget() != sequence.get(i + 1)) {
                        //throw new RuntimeException("Goto target is not the next node in the sequence");
                        System.err.println("Goto target is not the next node in the sequence");
                        result.add(new InstructionASTNode(goto_));
                    }
                } else {
                    @Nullable SpecialEdge edge = cfg.edgeFrom(sequence.get(sequence.size() - 1));

                    if (edge == null) {
                        if (goto_.getTarget() != this.getSortSuccessor()) {
                            //throw new RuntimeException("Invalid GOTO");
                            System.err.println("Invalid GOTO");
                            result.add(new InstructionASTNode(goto_));
                        }
                    } else {
                        result.add(edge.getASTNode());
                    }
                }
            } else if (node.getInstruction() instanceof ReturnInstruction returnInstruction) {
                result.add(new ReturnASTNode(returnInstruction.getValue()));
            } else {
                result.add(new InstructionASTNode(node.getInstruction()));
            }
        }

        return result;
    }
}
