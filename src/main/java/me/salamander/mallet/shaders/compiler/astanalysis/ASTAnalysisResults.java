package me.salamander.mallet.shaders.compiler.astanalysis;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import me.salamander.mallet.shaders.compiler.analysis.SemiLattice;
import me.salamander.mallet.shaders.compiler.analysis.Value;
import me.salamander.mallet.shaders.compiler.ast.node.ASTNode;
import me.salamander.mallet.util.Util;

import java.util.Map;

public class ASTAnalysisResults<T extends Value> {
    public final Map<ASTNode, T> in = new Object2ObjectOpenCustomHashMap<>(Util.IDENTITY_HASH_STRATEGY);
    public final Map<ASTNode, T> out = new Object2ObjectOpenCustomHashMap<>(Util.IDENTITY_HASH_STRATEGY);

    public ASTAnalysisResults(SemiLattice<T> semiLattice, ASTNode root) {
        if (semiLattice.getOrder() != SemiLattice.Order.FORWARDS) {
            throw new IllegalArgumentException("SemiLattice must be FORWARDS");
        }

        root.visitTree(node -> {
            in.put(node, semiLattice.getTop());
            out.put(node, semiLattice.getTop());
        });

        in.put(root, semiLattice.getHeadValue());
    }
}
