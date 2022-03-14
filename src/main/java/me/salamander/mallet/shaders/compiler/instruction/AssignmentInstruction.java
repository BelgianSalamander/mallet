package me.salamander.mallet.shaders.compiler.instruction;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AssignmentInstruction implements Instruction {
    private Location location;
    private Value value;

    public AssignmentInstruction(Location location, Value value) {
        this.location = location;
        this.value = value;
    }

    public Location getLocation() {
        return location;
    }

    public Value getValue() {
        return value;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return location + " = " + value;
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> used = new ArrayList<>(value.usedVariables());

        if(!(location instanceof Variable)) {
            //This makes sure that for stuff like var1.x = 2, the variable var1 is added to the list of live variables
            used.addAll(location.usedVariables());
        }

        return used;
    }

    @Override
    public Instruction visitAndReplace(Function<Value, Value> valueCopier, Function<Location, Location> locationCopier) {
        return new AssignmentInstruction(locationCopier.apply(location), valueCopier.apply(value));
    }

    @Override
    public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        location.writeGLSL(sb, ctx, shaderCompiler);
        sb.append(" = ");
        value.writeGLSL(sb, ctx, shaderCompiler);
        sb.append(";\n");
    }
}
