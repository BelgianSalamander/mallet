package me.salamander.mallet.compiler.instruction;

public record Label(String name){
    public String toString() {
        return name;
    }
}
