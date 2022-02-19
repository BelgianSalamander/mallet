package me.salamander.mallet.compiler.analysis.mutability;

public enum Mutability {
    IMMUTABLE,
    PASSIVE_MUTABLE,
    MUTABLE;

    public Mutability merge(Mutability other){
        if(this == other) return this;

        return IMMUTABLE;
    }
}
