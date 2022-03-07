package me.salamander.mallet.compiler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.salamander.mallet.annotation.MutatesSelf;
import me.salamander.mallet.annotation.Out;
import me.salamander.mallet.annotation.ReturnMutable;
import me.salamander.mallet.compiler.analysis.defined.DefinedSemilattice;
import me.salamander.mallet.compiler.analysis.defined.DefinedValue;
import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.astanalysis.ASTAnalysis;
import me.salamander.mallet.compiler.astanalysis.ASTAnalysisResults;
import me.salamander.mallet.compiler.constant.Constant;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.value.*;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.MethodInvocation;
import me.salamander.mallet.util.Ref;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ShaderCompiler {
    private final GlobalCompilationContext globalContext;
    private final Type mainClass;

    private final Set<StaticField> globalState = new HashSet<>();

    public ShaderCompiler(GlobalCompilationContext globalContext, Type mainClass) {
        this.globalContext = globalContext;
        this.mainClass = mainClass;
    }

    public ShaderCompiler(GlobalCompilationContext globalContext, Class<?> mainClass) {
        this(globalContext, Type.getType(mainClass));
    }

    public String compile(Object... mainArgs) {
        JavaDecompiler mainDecompiler = makeMainDecompiler();
        ASTNode astRoot = mainDecompiler.decompile();
        MethodNode method = mainDecompiler.getMethodNode();
        Type[] args = Type.getArgumentTypes(method.desc);

        Int2ObjectMap<Object> inlineArgs = new Int2ObjectOpenHashMap<>();
        int varIndex = 0;
        for(int i = 0; i < mainArgs.length; i++) {
            inlineArgs.put(varIndex, mainArgs[i]);
            varIndex += args[i].getSize();
        }
        astRoot = inlineConstants(method, astRoot, inlineArgs);

        StringBuilder sb = new StringBuilder();
        astRoot.print(sb);
        System.out.println(sb);

        return "";
    }

    private ASTNode inlineConstants(MethodNode methodNode, ASTNode decompiled, Int2ObjectMap<Object> args) {
        ASTAnalysisResults<DefinedValue> definedValues = ASTAnalysis.analyseMethod(new DefinedSemilattice(), decompiled);

        Ref<ASTNode> curr = new Ref<>(null);

        return inlineConstants(definedValues, decompiled, args, curr);
    }

    private ASTNode inlineConstants(ASTAnalysisResults<DefinedValue> definedValues, ASTNode decompiled, Int2ObjectMap<Object> args, Ref<ASTNode> curr) {
        curr.value = decompiled;

        Function<Value, Value> valueCopier = (val) -> inlineValueConstant(val, definedValues, args, curr.value);

        return decompiled.copy(
                node -> inlineConstants(definedValues, node, args, curr),
                insn -> {
                    return insn.copy(
                            valueCopier,
                            loc -> (Location) loc.copyValue(valueCopier)
                    );
                },
                valueCopier
        );
    }

    private Value inlineValueConstant(Value value, ASTAnalysisResults<DefinedValue> definedValues, Int2ObjectMap<Object> args, ASTNode insn) {
        value = value.copyValue(
                (node) -> inlineValueConstant(node, definedValues, args, insn)
        );

        if (value instanceof Variable var) {
            if (var.getVariableType() == VariableType.LOCAL) {
                int index = var.getIndex();
                if (!definedValues.in.get(insn).getDefinedVars().contains(var)) {
                    return new Constant(args.get(index), var);
                }
            }
        }

        return value;
    }

    private JavaDecompiler makeMainDecompiler() {
        ClassNode mainClassNode = globalContext.findClass(mainClass);

        //Find global state
        for(FieldNode fieldNode : mainClassNode.fields) {
            if(ASMUtil.isStatic(fieldNode.access)) {
                globalState.add(new StaticField(mainClass, fieldNode.name, Type.getMethodType(fieldNode.desc)));
            }else{
                throw new RuntimeException("Non-static fields are not supported in main class");
            }
        }

        boolean found = false;
        MethodNode method = null;
        for(MethodNode methodNode : mainClassNode.methods) {
            if(methodNode.name.equals("main") && (methodNode.access & Opcodes.ACC_STATIC) != 0) {
                if(found) {
                    throw new IllegalStateException("Multiple main methods found");
                }

                method = methodNode;
                found = true;
            }
        }

        if(!found) {
            throw new IllegalStateException("No main method found");
        }

        return new JavaDecompiler(mainClassNode, method, this);
    }

    public JavaDecompiler makeDecompiler(Type methodOwner, String methodName, Type methodDesc) {
        ClassNode classNode = globalContext.findClass(methodOwner);
        MethodNode methodNode = ASMUtil.findMethod(classNode, methodName, methodDesc);

        return new JavaDecompiler(classNode, methodNode, this);
    }

    public Set<StaticField> getGlobalState() {
        return globalState;
    }

    /**
     * Returns all the arguments of the method that get mutated.
     * @param invocation The method to get info for
     * @return The list of arguments that get mutated. One of those arguments may be -1 if the method transforms the global state.
     */

    //TODO: Detect what global state gets mutated
    public IntList getMutatedArgs(MethodInvocation invocation) {
        IntList mutatedArgs = new IntArrayList();

        ClassNode classNode = globalContext.findClass(invocation.methodOwner());
        MethodNode methodNode = ASMUtil.findMethod(classNode, invocation.methodName(), invocation.methodDesc());

        if(modifiesShaderVars(invocation, methodNode)) {
            mutatedArgs.add(-1);
        }

        if(invocation.methodName().equals("<init>")) {
            //Constructors always mutate their first arg
            mutatedArgs.add(0);
        }

        //Check for @MutatesSelf annotations
        if(methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                if (annotation.desc.equals(MutatesSelf.class.descriptorString())) {
                    if (invocation.type() != MethodInvocation.MethodCallType.STATIC) {
                        mutatedArgs.add(0);
                    } else {
                        throw new IllegalStateException("@MutatesSelf annotation is only valid on non-static methods");
                    }
                }
            }
        }

        //Check for @Out annotations on parameters
        if(methodNode.visibleParameterAnnotations == null){
            return mutatedArgs;
        }

        int offset = invocation.type() == MethodInvocation.MethodCallType.STATIC ? 0 : 1;
        for (int i = 0; i < methodNode.visibleParameterAnnotations.length; i++) {
            List<AnnotationNode> annotations = methodNode.visibleParameterAnnotations[i];

            if(annotations != null) {
                for (AnnotationNode annotation : annotations) {
                    if(annotation.desc.equals(Out.class.descriptorString())) {
                        mutatedArgs.add(i + offset);
                        break;
                    }
                }
            }
        }

        return mutatedArgs;
    }

    private boolean modifiesShaderVars(MethodInvocation invocation, MethodNode method) {
        if(invocation.methodOwner().equals(mainClass)) {
            //Check if the method contains any PUTSTATIC instructions into the shader variables

            for (AbstractInsnNode instruction : method.instructions) {
                if(instruction.getOpcode() == Opcodes.PUTSTATIC) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;

                    if(fieldInsn.owner.equals(mainClass.getInternalName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean returnsMutable(MethodInvocation invocation) {
        ClassNode classNode = globalContext.findClass(invocation.methodOwner());
        MethodNode methodNode = ASMUtil.findMethod(classNode, invocation.methodName(), invocation.methodDesc());

        if(methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                if (annotation.desc.equals(ReturnMutable.class.descriptorString())) {
                    return true;
                }
            }
        }

        return false;
    }
}
