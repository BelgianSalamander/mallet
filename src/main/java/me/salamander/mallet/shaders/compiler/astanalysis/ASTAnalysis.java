package me.salamander.mallet.shaders.compiler.astanalysis;

import me.salamander.mallet.shaders.compiler.analysis.SemiLattice;
import me.salamander.mallet.shaders.compiler.analysis.Value;
import me.salamander.mallet.shaders.compiler.ast.node.*;
import me.salamander.mallet.shaders.compiler.instruction.ReturnInstruction;

import java.util.List;

public class ASTAnalysis {
    public static <T extends Value> ASTAnalysisResults<T> analyseMethod(SemiLattice<T> semiLattice, ASTNode root) {
        MethodASTNode method = (MethodASTNode) root;

        ASTAnalysisResults<T> results = new ASTAnalysisResults<T>(semiLattice, root);

        while (analyseMethod(semiLattice, method, results)) {
            // Do nothing
        }

        return results;
    }

    private static <T extends Value> boolean analyseMethod(SemiLattice<T> semiLattice, MethodASTNode method, ASTAnalysisResults<T> results) {
        List<ASTNode> body = method.getBody();
        boolean changed = false;

        if (!results.in.get(method).equals(results.in.get(body.get(0)))) {
            changed = true;
            results.in.put(method, results.in.get(body.get(0)));
        }

        if (analyseList(semiLattice, body, results)) {
            changed = true;
        }

        T out = results.out.get(method);
        if (!out.equals(results.out.get(method))) {
            changed = true;
            results.out.put(method, out);
        }

        return changed;
    }

    private static <T extends Value> boolean analyseList(SemiLattice<T> semiLattice, List<ASTNode> body, ASTAnalysisResults<T> results) {
        boolean changed = false;

        for (int i = 0; i < body.size(); i++) {
            ASTNode node = body.get(i);

            if (analyseNode(semiLattice, node, results)) {
                changed = true;
            }

            if (i < body.size() - 1) {
                if (!results.out.get(node).equals(results.in.get(body.get(i + 1)))) {
                    changed = true;
                    results.in.put(body.get(i + 1), results.out.get(node));
                }
            }
        }

        return changed;
    }

    private static <T extends Value> boolean analyseNode(SemiLattice<T> semiLattice, ASTNode node, ASTAnalysisResults<T> results) {
        if (node instanceof BreakASTNode || node instanceof ContinueASTNode) {
            return false;
        }

        if (node instanceof IfASTNode ifNode) {
            return analyseIf(semiLattice, ifNode, results);
        } else if (node instanceof IfElseASTNode ifElseNode) {
            return analyseIfElse(semiLattice, ifElseNode, results);
        } else if (node instanceof InstructionASTNode instructionNode) {
            return analyseInstruction(semiLattice, instructionNode, results);
        } else if (node instanceof LabelledASTBlock labelledBlock) {
            return analyseLabelledBlock(semiLattice, labelledBlock, results);
        } else if (node instanceof ReturnASTNode returnNode) {
            return analyseReturn(semiLattice, returnNode, results);
        }

        throw new RuntimeException("Unknown ASTNode type: " + node.getClass().getName());
    }

    private static <T extends Value> boolean analyseReturn(SemiLattice<T> semiLattice, ReturnASTNode returnNode, ASTAnalysisResults<T> results) {
        T newValue = semiLattice.execute(results.in.get(returnNode), new ReturnInstruction(returnNode.getReturnValue()));

        if (!newValue.equals(results.out.get(returnNode))) {
            results.out.put(returnNode, newValue);
            return true;
        }

        return false;
    }

    private static <T extends Value> boolean analyseInstruction(SemiLattice<T> semiLattice, InstructionASTNode instructionNode, ASTAnalysisResults<T> results) {
        T newValue = semiLattice.execute(results.in.get(instructionNode), instructionNode.getInstruction());

        if (!newValue.equals(results.out.get(instructionNode))) {
            results.out.put(instructionNode, newValue);
            return true;
        }

        return false;
    }

    private static <T extends Value> boolean analyseIf(SemiLattice<T> semiLattice, IfASTNode ifNode, ASTAnalysisResults<T> results) {
        List<ASTNode> body = ifNode.getBody();
        boolean changed = false;

        if (!results.in.get(ifNode).equals(results.in.get(body.get(0)))) {
            changed = true;
            results.in.put(ifNode, results.in.get(body.get(0)));
        }

        if (analyseList(semiLattice, body, results)) {
            changed = true;
        }

        T conditionalOut = results.out.get(body.get(body.size() - 1));
        T mergedOut = semiLattice.meet(conditionalOut, results.in.get(ifNode));

        if (!mergedOut.equals(results.out.get(ifNode))) {
            changed = true;
            results.out.put(ifNode, mergedOut);
        }

        return changed;
    }

    private static <T extends Value> boolean analyseIfElse(SemiLattice<T> semiLattice, IfElseASTNode ifElseNode, ASTAnalysisResults<T> results) {
        List<ASTNode> branchOne = ifElseNode.getIfTrue();
        List<ASTNode> branchTwo = ifElseNode.getIfFalse();
        boolean changed = false;

        if (!results.in.get(ifElseNode).equals(results.in.get(branchOne.get(0)))) {
            changed = true;
            results.in.put(ifElseNode, results.in.get(branchOne.get(0)));
        }

        if (!results.in.get(ifElseNode).equals(results.in.get(branchTwo.get(0)))) {
            changed = true;
            results.in.put(ifElseNode, results.in.get(branchTwo.get(0)));
        }

        if (analyseList(semiLattice, branchOne, results)) {
            changed = true;
        }

        if (analyseList(semiLattice, branchTwo, results)) {
            changed = true;
        }

        T out = semiLattice.meet(results.out.get(branchOne.get(branchOne.size() - 1)), results.out.get(branchTwo.get(branchTwo.size() - 1)));

        if (!out.equals(results.out.get(ifElseNode))) {
            changed = true;
            results.out.put(ifElseNode, out);
        }

        return changed;
    }

    private static <T extends Value> boolean analyseLabelledBlock(SemiLattice<T> semiLattice, LabelledASTBlock labelledBlock, ASTAnalysisResults<T> results) {
        List<ASTNode> body = labelledBlock.getBody();
        boolean changed = false;

        if (!results.in.get(labelledBlock).equals(results.in.get(body.get(0)))) {
            changed = true;
            results.in.put(labelledBlock, results.in.get(body.get(0)));
        }

        if (analyseList(semiLattice, body, results)) {
            changed = true;
        }

        T conditionalOut = results.out.get(body.get(body.size() - 1));
        T mergedOut = semiLattice.meet(conditionalOut, results.in.get(labelledBlock));

        if (!mergedOut.equals(results.out.get(labelledBlock))) {
            changed = true;
            results.out.put(labelledBlock, mergedOut);
        }

        return changed;
    }
}
