package me.salamander.mallet.compiler.analysis;

import me.salamander.mallet.compiler.cfg.BasicBlock;
import me.salamander.mallet.compiler.cfg.IntermediaryCFG;

import java.util.List;

//Dragon book, chapter 9
public class Analysis {
    public static <T extends Value> AnalysisResults<T> analyze(SemiLattice<T> semiLattice, IntermediaryCFG cfg) {
        if(semiLattice.getOrder() == SemiLattice.Order.FORWARDS) {
            return analyzeForwards(semiLattice, cfg);
        }else{
            return analyzeBackwards(semiLattice, cfg);
        }
    }

    private static <T extends Value> AnalysisResults<T> analyzeForwards(SemiLattice<T> semiLattice, IntermediaryCFG cfg) {
        AnalysisResults<T> results = new AnalysisResults<>(semiLattice, cfg);
        int headIndex = semiLattice.getHeadIndex(cfg);
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < cfg.getBlocks().size(); i++) {
                if (i == headIndex) continue;

                BasicBlock block = cfg.getBlocks().get(i);

                T inValue = semiLattice.getTop();
                for (BasicBlock previous : block.getPrev()) {
                    inValue = semiLattice.meet(inValue, results.getIntraBlockValues().out[cfg.getIndexOf(previous)]);
                }
                results.getIntraBlockValues().in[i] = inValue;

                T outValue = analyzeBlockForwards(semiLattice, block, inValue, results.getBlocks().get(block));
                T prevOutValue = results.getIntraBlockValues().out[i];

                if(!outValue.equals(prevOutValue)) {
                    changed = true;
                }

                results.getIntraBlockValues().out[i] = outValue;
            }
        }

        return results;
    }

    private static <T extends Value> T analyzeBlockForwards(SemiLattice<T> semiLattice, BasicBlock block, T inValue, AnalysisInfo<T> analysisInfo) {
        if(block.getInstructions().isEmpty()) return inValue;

        analysisInfo.in[0] = inValue;

        for (int i = 0; i < block.getInstructions().size(); i++) {
            analysisInfo.out[i] = semiLattice.execute(analysisInfo.in[i], block.getInstructions().get(i));
            if(i < block.getInstructions().size() - 1) {
                analysisInfo.in[i + 1] = analysisInfo.out[i];
            }
        }

        return analysisInfo.out[block.getInstructions().size() - 1];
    }

    private static <T extends Value> AnalysisResults<T> analyzeBackwards(SemiLattice<T> semiLattice, IntermediaryCFG cfg) {
        AnalysisResults<T> results = new AnalysisResults<>(semiLattice, cfg);
        int headIndex = 0;
        boolean changed = true;

        List<BasicBlock> backwardsBlocks = cfg.getEndBlock().getAllBackwards();

        while (changed) {
            changed = false;
            for (int i = 0; i < backwardsBlocks.size(); i++) {
                if (i == headIndex) continue;

                BasicBlock block = backwardsBlocks.get(i);

                T outValue = semiLattice.getTop();
                for (BasicBlock next : block.getNext()) {
                    int backwardsIndex = backwardsBlocks.indexOf(next);
                    outValue = semiLattice.meet(outValue, results.getIntraBlockValues().in[backwardsIndex]);
                }
                results.getIntraBlockValues().out[i] = outValue;

                T inValue = analyzeBlockBackwards(semiLattice, block, outValue, results.getBlocks().get(block));
                T prevInValue = results.getIntraBlockValues().in[i];

                if(!inValue.equals(prevInValue)) {
                    changed = true;
                }

                results.getIntraBlockValues().in[i] = inValue;
            }
        }

        return results;
    }

    private static <T extends Value> T analyzeBlockBackwards(SemiLattice<T> semiLattice, BasicBlock block, T outValue, AnalysisInfo<T> analysisInfo) {
        if(block.getInstructions().isEmpty()) return outValue;

        int lastIndex = block.getInstructions().size() - 1;

        analysisInfo.out[lastIndex] = outValue;

        for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
            analysisInfo.in[i] = semiLattice.execute(analysisInfo.out[i], block.getInstructions().get(i));
            if(i > 0) {
                analysisInfo.out[i - 1] = analysisInfo.in[i];
            }
        }

        return analysisInfo.in[0];
    }
}
