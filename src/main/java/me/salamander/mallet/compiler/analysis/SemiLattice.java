package me.salamander.mallet.compiler.analysis;

import me.salamander.mallet.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.compiler.instruction.Instruction;

public abstract class SemiLattice<T extends Value> {
    private final Order order;

    protected SemiLattice(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    //The head is either the entry or exit of the CFG depending on the order
    public int getHeadIndex(IntermediaryCFG cfg){
        return this.getHeadIndex(cfg.getStartIndex(), cfg.getEndIndex());
    }

    public int getHeadIndex(int startIndex, int endIndex){
        return order == Order.FORWARDS ? startIndex : endIndex;
    }

    public abstract T getHeadValue();
    public abstract T getTop();

    public abstract T execute(T value, Instruction instruction);
    public abstract T meet(T a, T b);

    public abstract T[] makeArray(int size);

    public enum Order{
        FORWARDS,
        BACKWARDS
    }
}
