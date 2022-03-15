package me.salamander.mallet.shaders.compiler;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.compiler.analysis.defined.DefinedSemilattice;
import me.salamander.mallet.shaders.compiler.analysis.defined.DefinedValue;
import me.salamander.mallet.shaders.compiler.analysis.definition.DefinitionSemilattice;
import me.salamander.mallet.shaders.compiler.analysis.definition.DefinitionValue;
import me.salamander.mallet.shaders.compiler.ast.ASTVisitor;
import me.salamander.mallet.shaders.compiler.ast.node.*;
import me.salamander.mallet.shaders.compiler.astanalysis.ASTAnalysis;
import me.salamander.mallet.shaders.compiler.astanalysis.ASTAnalysisResults;
import me.salamander.mallet.shaders.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.value.Location;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import me.salamander.mallet.shaders.compiler.instruction.value.VariableType;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.util.Ref;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompiledMethod {
    private final ASTNode root;
    private final String name;
    private final UUID uuid;
    private final Type returnType;
    private final Type[] argumentTypes;
    private final int[] argumentIndices;
    private final boolean[] outArguments;

    public CompiledMethod(ASTNode root, Type owner ,String name, Type returnType, Type[] argumentTypes, int[] argumentIndices, boolean[] outArguments) {
        Ref<Function<Value, Value>> valueCopier = new Ref<>(null);
        valueCopier.value = (value) -> value.copyValue(valueCopier.value);

        Ref<Function<ASTNode, ASTNode>> nodeCopier = new Ref<>(null);
        nodeCopier.value = node -> node.visitAndReplace(nodeCopier.value, (insn) -> insn.visitAndReplace(valueCopier.value, l -> (Location) l.copyValue(valueCopier.value)), valueCopier.value);

        this.root = nodeCopier.value.apply(root);
        this.uuid = UUID.randomUUID();
        this.name = Util.removeSpecial( owner + "_" + name + "_" + uuid);

        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
        this.argumentIndices = argumentIndices;
        this.outArguments = outArguments;

        replaceArgTypes();
    }

    public CompiledMethod(ASTNode root, String name, Type returnType, Type[] argumentTypes, boolean scrambleName) {
        this.root = root;
        this.uuid = UUID.randomUUID();
        if (scrambleName) {
            this.name = Util.removeSpecial(name + uuid);
        } else {
            this.name = Util.removeSpecial(name);
        }

        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
        this.argumentIndices = new int[0];
        this.outArguments = new boolean[0];
    }

    private void replaceArgTypes() {
        ASTAnalysisResults<DefinedValue> results = ASTAnalysis.analyseMethod(new DefinedSemilattice(), root);

        Ref<ASTNode> currInstruction = new Ref<>(null);

        Int2IntMap localVarToParam = new Int2IntOpenHashMap();

        int localVarIndex = 0;
        int paramIndex = 0;
        for (Type argumentType : argumentTypes) {
            localVarToParam.put(localVarIndex, paramIndex);
            localVarIndex += argumentType.getSize();
            paramIndex++;
        }

        Ref<Function<Value, Value>> valueCopier = new Ref<>(null);
        valueCopier.value = (value) -> {
            if (value instanceof Variable var) {
                if(var.getVariableType() == VariableType.LOCAL) {
                    if (!results.in.get(currInstruction.value).getDefinedVars().contains(var)) {
                        int param = localVarToParam.get(var.getIndex());

                        if(!var.getType().equals(argumentTypes[param])) {
                            return new Variable(argumentTypes[param], var.getIndex(), var.getVariableType());
                        }
                    }
                }
            }

            return value;
        };

        root.visitTree((node) -> {
            if (node instanceof InstructionASTNode insnNode) {
                currInstruction.value = insnNode;
                insnNode.setInstruction(insnNode.getInstruction().visitAndReplace(
                        valueCopier.value,
                        loc -> (Location) loc.copyValue(valueCopier.value)
                ));
            }
        });

        repairVars();
    }

    private void repairVars() {
        Ref<Boolean> changed = new Ref<>(false);
        Ref<Boolean> hasIncompatibleMerges = new Ref<>(false);

        while (changed.value) {
            changed.value = false;
            hasIncompatibleMerges.value = false;

            ASTAnalysisResults<DefinitionValue> results = ASTAnalysis.analyseMethod(new DefinitionSemilattice(argumentTypes), root);

            Ref<ASTNode> currInstruction = new Ref<>(null);
            Ref<Function<Value, Value>> valueCopier = new Ref<>(null);
            valueCopier.value = (value) -> {
                if (value instanceof Variable var) {
                    Set<Instruction> definitions = results.in.get(currInstruction.value).get(var);
                    Set<Type> possibleTypes = definitions.stream()
                            .map(insn -> ((AssignmentInstruction) insn).getValue().getType())
                            .collect(Collectors.toSet());

                    if (possibleTypes.size() == 0) {
                        throw new RuntimeException("No possible types for variable " + var);
                    }

                    if (possibleTypes.size() > 1) {
                        hasIncompatibleMerges.value = true;
                    }

                    if (possibleTypes.contains(var.getType())) {
                        return value;
                    }

                    changed.value = true;
                    return new Variable(possibleTypes.iterator().next(), var.getIndex(), var.getVariableType());
                }

                return value;
            };

            root.visitTree((node) -> {
                if (node instanceof InstructionASTNode insnNode) {
                    currInstruction.value = insnNode;
                    insnNode.setInstruction(insnNode.getInstruction().visitAndReplace(
                            valueCopier.value,
                            loc -> (Location) loc.copyValue(valueCopier.value)
                    ));
                }
            });
        }
    }

    public ASTNode getRoot() {
        return root;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Type[] getArgumentTypes() {
        return argumentTypes;
    }

    public String getName() {
        return name;
    }

    private void writeSignature(StringBuilder sb, MalletContext ctx) {
        //Return type
        if (returnType == Type.VOID_TYPE) {
            sb.append("void ");
        } else {
            MalletType type = ctx.getType(returnType);
            sb.append(type.getName()).append(" ");
        }

        //Method name
        sb.append(name).append("(");

        //Arguments
        for (int i = 0; i < argumentTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            MalletType type = ctx.getType(argumentTypes[i]);

            if (outArguments[i]) {
                sb.append("out ");
            }

            sb.append(type.getName());
            sb.append(" ");
            sb.append(ctx.varName(new Variable(argumentTypes[i], argumentIndices[i], VariableType.LOCAL)));
        }

        sb.append(")");
    }

    public void declare(StringBuilder sb, MalletContext ctx) {
        this.writeSignature(sb, ctx);
        sb.append(";\n");
    }

    public void define(StringBuilder glsl, MalletContext ctx, ShaderCompiler shaderCompiler) {
        this.writeSignature(glsl, ctx);
        glsl.append(" {\n");

        //Get local vars
        Set<Variable> localVars = new ObjectOpenCustomHashSet<>(Variable.TYPED_STRATEGY);

        Ref<Function<Value, Value>> visitor = new Ref<>(null);
        visitor.value = (value) -> {
            if (value instanceof Variable var) {
                localVars.add(var);
            }

            return value;
        };

        root.visitTree(node -> {
            if (node instanceof InstructionASTNode insnNode) {
                insnNode.getInstruction().visitAndReplace(
                        visitor.value,
                        (loc) -> (Location) visitor.value.apply(loc)
                );
            }
        });

        Set<Variable> excludeDeclarations = new HashSet<>();
        for (int i = 0; i < argumentTypes.length; i++) {
            excludeDeclarations.add(new Variable(argumentTypes[i], argumentIndices[i], VariableType.LOCAL));
        }

        localVars.removeAll(excludeDeclarations);

        //Declare local vars
        for (Variable var : localVars) {
            if (var.getType() != null) {
                MalletType type = ctx.getType(var.getType());
                glsl.append(Util.indent);
                glsl.append(type.getName()).append(" ");
                glsl.append(ctx.varName(var)).append(";\n");
            }
        }

        MethodASTNode method = (MethodASTNode) root;

        method.visit(makePrinter(glsl, ctx, shaderCompiler));

        glsl.append("}\n\n");
    }

    private ASTVisitor makePrinter(StringBuilder sb, MalletContext ctx, ShaderCompiler shaderCompiler) {
        return new ASTVisitor() {
            private int indent = 0;

            private String indentString() {
                return Util.indent.repeat(indent);
            }

            @Override
            public void visitBreak(BreakASTNode node) {
                sb.append(indentString());
                sb.append("break");

                if (node.needsLabel()) {
                    sb.append(" ");
                    sb.append(node.getLabel());
                }

                sb.append(";\n");
            }

            @Override
            public void visitContinue(ContinueASTNode node) {
                sb.append(indentString());
                sb.append("continue");

                if (node.needsLabel()) {
                    sb.append(" ");
                    sb.append(node.getLabel());
                }

                sb.append(";\n");
            }

            @Override
            public void visitReturn(ReturnASTNode node) {
                sb.append(indentString());
                sb.append("return");

                if (node.getReturnValue() != null) {
                    sb.append(" ");
                    node.getReturnValue().writeGLSL(sb, ctx, shaderCompiler);
                }

                sb.append(";\n");
            }

            @Override
            public void visitInstruction(InstructionASTNode node) {
                sb.append(indentString());
                node.getInstruction().writeGLSL(sb, ctx, shaderCompiler);
            }

            @Override
            public void enterIf(IfASTNode node) {
                sb.append(indentString());
                sb.append("if (");
                node.getCondition().writeGLSL(sb, ctx, shaderCompiler);
                sb.append(") {\n");
                indent++;
            }

            @Override
            public void exitIf(IfASTNode node) {
                indent--;
                sb.append(indentString());
                sb.append("}\n");
            }

            @Override
            public void enterIfElseTrueBody(IfElseASTNode node) {
                sb.append(indentString());
                sb.append("if (");
                node.getCondition().writeGLSL(sb, ctx, shaderCompiler);
                sb.append(") {\n");
                indent++;
            }

            @Override
            public void enterIfElseFalseBody(IfElseASTNode node) {
                indent--;
                sb.append(indentString());
                sb.append("} else {\n");
                indent++;
            }

            @Override
            public void exitIfElse(IfElseASTNode node) {
                indent--;
                sb.append(indentString());
                sb.append("}\n");
            }

            @Override
            public void enterLoop(LoopASTNode node) {
                sb.append(indentString());
                if (node.needsLabel()) {
                    throw new IllegalStateException("Cannot have labels in GLSL");
                }
                sb.append("while (");
                node.getCondition().writeGLSL(sb, ctx, shaderCompiler);
                sb.append(") {\n");
                indent++;
            }

            @Override
            public void exitLoop(LoopASTNode node) {
                indent--;
                sb.append(indentString());
                sb.append("}\n");
            }

            @Override
            public void enterLabelledBlock(LabelledBlockASTNode node) {
                sb.append(indentString());
                if (node.needsLabel()) {
                    throw new IllegalStateException("Cannot have labels in GLSL");
                }
                sb.append("{\n");
                indent++;
            }

            @Override
            public void exitLabelledBlock(LabelledBlockASTNode node) {
                indent--;
                sb.append(indentString());
                sb.append("}\n");
            }

            @Override
            public void enterMethod(MethodASTNode node) {
                indent++;
            }

            @Override
            public void exitMethod(MethodASTNode node) {
                indent--;
            }
        };
    }
}
