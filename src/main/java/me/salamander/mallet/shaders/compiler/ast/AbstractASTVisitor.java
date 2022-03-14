package me.salamander.mallet.shaders.compiler.ast;

import me.salamander.mallet.shaders.compiler.ast.node.*;

public abstract class AbstractASTVisitor implements ASTVisitor {
    @Override
    public void visitBreak(BreakASTNode node) {

    }

    @Override
    public void visitContinue(ContinueASTNode node) {

    }

    @Override
    public void visitReturn(ReturnASTNode node) {

    }

    @Override
    public void visitInstruction(InstructionASTNode node) {

    }

    @Override
    public void enterIf(IfASTNode node) {

    }

    @Override
    public void exitIf(IfASTNode node) {

    }

    @Override
    public void enterIfElseTrueBody(IfElseASTNode node) {

    }

    @Override
    public void enterIfElseFalseBody(IfElseASTNode node) {

    }

    @Override
    public void exitIfElse(IfElseASTNode node) {

    }

    @Override
    public void enterLoop(LoopASTNode node) {

    }

    @Override
    public void exitLoop(LoopASTNode node) {

    }

    @Override
    public void enterLabelledBlock(LabelledBlockASTNode node) {

    }

    @Override
    public void exitLabelledBlock(LabelledBlockASTNode node) {

    }

    @Override
    public void enterMethod(MethodASTNode node) {

    }

    @Override
    public void exitMethod(MethodASTNode node) {

    }
}
