package me.salamander.mallet.shaders.compiler.ast.make.cfg;

import me.salamander.mallet.shaders.compiler.ast.node.ASTNode;
import me.salamander.mallet.shaders.compiler.ast.node.BreakASTNode;
import me.salamander.mallet.shaders.compiler.ast.node.ContinueASTNode;
import me.salamander.mallet.shaders.compiler.ast.make.set.SETNode;

public record SpecialEdge(SETNode target, Type type) {
    public ASTNode getASTNode() {
        if(this.type == Type.CONTINUE) {
            return new ContinueASTNode(target.getLabel());
        } else {
            return new BreakASTNode(target.getLabel());
        }
    }

    public enum Type {
        CONTINUE,
        BREAK;
    }
}
