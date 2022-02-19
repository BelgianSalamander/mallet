package me.salamander.mallet.compiler;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.salamander.mallet.annotation.MutatesSelf;
import me.salamander.mallet.annotation.Out;
import me.salamander.mallet.compiler.instruction.value.StaticField;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.MethodInvocation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        mainDecompiler.decompile();

        return "";
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
}
