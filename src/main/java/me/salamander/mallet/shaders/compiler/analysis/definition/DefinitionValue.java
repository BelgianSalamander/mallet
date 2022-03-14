package me.salamander.mallet.shaders.compiler.analysis.definition;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.ShaderCompiler;
import me.salamander.mallet.shaders.compiler.analysis.Value;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import me.salamander.mallet.shaders.compiler.instruction.value.VariableType;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;

public class DefinitionValue extends Value {
    public static final Instruction ARG_PLACEHOLDER = new Instruction() {
        @Override
        public List<Variable> usedVariables() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instruction visitAndReplace(Function<me.salamander.mallet.shaders.compiler.instruction.value.Value, me.salamander.mallet.shaders.compiler.instruction.value.Value> valueCopier, Function<Location, Location> locationCopier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeGLSL(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
            throw new UnsupportedOperationException();
        }
    };
    private final Map<Variable, Set<Instruction>> definitionLocations;

    public DefinitionValue(int... argVarIndices) {
        this.definitionLocations = new HashMap<>();

        for (int argVarIndex : argVarIndices) {
            Variable argVar = new Variable(Type.INT_TYPE, argVarIndex, VariableType.LOCAL);
        }
    }

    public DefinitionValue(Map<Variable, Set<Instruction>> definitionLocations) {
        this.definitionLocations = definitionLocations;
    }

    public DefinitionValue set(Variable variable, Instruction definition) {
        Map<Variable, Set<Instruction>> newDefinitionLocations = new HashMap<>(definitionLocations);
        newDefinitionLocations.put(variable, Set.of(definition));
        return new DefinitionValue(newDefinitionLocations);
    }

    public DefinitionValue merge(DefinitionValue other) {
        Map<Variable, Set<Instruction>> newDefinitionLocations = new HashMap<>(definitionLocations);

        for (Map.Entry<Variable, Set<Instruction>> entry : other.definitionLocations.entrySet()) {
            Variable variable = entry.getKey();

            if (newDefinitionLocations.containsKey(variable)) {
                newDefinitionLocations.put(
                        variable,
                        Util.union(newDefinitionLocations.get(variable), entry.getValue())
                );
            }
        }

        return new DefinitionValue(newDefinitionLocations);
    }

    public Set<Instruction> get(Variable variable) {
        return definitionLocations.getOrDefault(variable, Set.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefinitionValue that = (DefinitionValue) o;
        return Objects.equals(definitionLocations, that.definitionLocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionLocations);
    }
}
