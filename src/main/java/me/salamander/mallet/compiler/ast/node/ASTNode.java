package me.salamander.mallet.compiler.ast.node;

import java.io.PrintStream;

public abstract class ASTNode {
    public abstract void print(StringBuilder sb, String indent);

    public void print(StringBuilder sb) {
        print(sb, "");
    }
}
