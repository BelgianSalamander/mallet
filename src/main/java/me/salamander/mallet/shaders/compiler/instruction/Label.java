package me.salamander.mallet.shaders.compiler.instruction;

public record Label(String name){
    public String toString() {
        return name;
    }
}
