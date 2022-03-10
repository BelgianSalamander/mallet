package me.salamander.mallet.compiler.ast;

import me.salamander.mallet.compiler.ast.node.*;

public interface ASTVisitor {
    void visitBreak(BreakASTNode node);
    void visitContinue(ContinueASTNode node);
    void visitReturn(ReturnASTNode node);
    void visitInstruction(InstructionASTNode node);

    void enterIf(IfASTNode node);
    void exitIf(IfASTNode node);

    void enterIfElseTrueBody(IfElseASTNode node);
    void enterIfElseFalseBody(IfElseASTNode node);
    void exitIfElse(IfElseASTNode node);

    void enterLoop(LoopASTNode node);
    void exitLoop(LoopASTNode node);

    void enterLabelledBlock(LabelledBlockASTNode node);
    void exitLabelledBlock(LabelledBlockASTNode node);

    void enterMethod(MethodASTNode node);
    void exitMethod(MethodASTNode node);
}
