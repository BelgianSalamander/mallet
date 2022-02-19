package me.salamander.mallet.compiler.analysis;

import me.salamander.mallet.compiler.cfg.BasicBlock;
import me.salamander.mallet.compiler.cfg.IntermediaryCFG;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class AnalysisResults<T extends Value> {
    private final Map<BasicBlock, AnalysisInfo<T>> blocks;
    private final AnalysisInfo<T> intraBlockValues;

    public AnalysisResults(SemiLattice<T> semiLattice, IntermediaryCFG cfg) {
        this.intraBlockValues = new AnalysisInfo<>(semiLattice, cfg.getBlocks().size(), 0);
        this.blocks = new HashMap<>();

        for (BasicBlock block : cfg.getBlocks()) {
            this.blocks.put(block, new AnalysisInfo<>(semiLattice, block.getInstructions().size(), -1));
        }
    }

    public Map<BasicBlock, AnalysisInfo<T>> getBlocks() {
        return blocks;
    }

    public AnalysisInfo<T> getIntraBlockValues() {
        return intraBlockValues;
    }

    public void print(PrintStream out, IntermediaryCFG controlFlowGraph) {
        out.println("Analysis Results:");

        for (BasicBlock block : controlFlowGraph.getBlocks()) {
            AnalysisInfo<T> info = this.blocks.get(block);

            for (int i = 0; i < block.getInstructions().size(); i++) {
                out.println(block.getInstructions().get(i) + " IN: " + info.in[i]/* + " OUT: " + info.out[i]*/);
            }
        }
    }
}
