package me.salamander.mallet.shaders.compiler.analysis;

public class AnalysisInfo<T extends Value> {
    public T[] in;
    public T[] out;

    public AnalysisInfo(SemiLattice<T> semiLattice, int length, int headIndex){
        in = semiLattice.makeArray(length);
        out = semiLattice.makeArray(length);

        for(int i = 0; i < length; i++){
            if(i == headIndex){
                if(semiLattice.getOrder() == SemiLattice.Order.FORWARDS) {
                    out[i] = semiLattice.getHeadValue();
                }else{
                    in[i] = semiLattice.getHeadValue();
                }
                continue;
            }

            in[i] = semiLattice.getTop();
            out[i] = semiLattice.getTop();
        }
    }
}
