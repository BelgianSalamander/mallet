package me.salamander.mallet.compiler.instruction.value;

public enum VariableType {
    LOCAL(""),
    STACK("s_"),
    SYNTHETIC("t_");

    private final String prefix;

    VariableType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
