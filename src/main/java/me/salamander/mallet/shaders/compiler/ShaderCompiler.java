package me.salamander.mallet.shaders.compiler;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.salamander.mallet.MalletContext;
import me.salamander.mallet.shaders.annotation.*;
import me.salamander.mallet.shaders.annotation.internal.ShaderVar;
import me.salamander.mallet.shaders.compiler.analysis.defined.DefinedSemilattice;
import me.salamander.mallet.shaders.compiler.analysis.defined.DefinedValue;
import me.salamander.mallet.shaders.compiler.ast.node.ASTNode;
import me.salamander.mallet.shaders.compiler.ast.node.InstructionASTNode;
import me.salamander.mallet.shaders.compiler.astanalysis.ASTAnalysis;
import me.salamander.mallet.shaders.compiler.astanalysis.ASTAnalysisResults;
import me.salamander.mallet.shaders.compiler.constant.Constant;
import me.salamander.mallet.shaders.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.shaders.compiler.instruction.Instruction;
import me.salamander.mallet.shaders.compiler.instruction.MethodCallInstruction;
import me.salamander.mallet.shaders.compiler.instruction.value.*;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.shaders.shader.ComputeShader;
import me.salamander.mallet.shaders.shader.Shader;
import me.salamander.mallet.util.*;
import org.joml.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShaderCompiler {
    //Placeholder hack until proper type possibility detection
    private static final boolean AUTO_CONVERT_JOML_ITF = true;
    private static final Map<Type, Type> AUTO_CONVERT = new HashMap<>();

    private final MalletContext globalContext;
    private final Type mainClass;

    private final Set<StaticField> globalState = new HashSet<>();
    private final Map<MethodInvocationWithConstants, CompiledMethod> compiledMethodMap = new HashMap<>();
    private final Set<PrimitiveConstant> constants = new HashSet<>();
    private final Set<PrimitiveConstant> needFullConstants = new HashSet<>();
    private final Object2ObjectMap<PrimitiveConstant, String> constantNames = new Object2ObjectOpenHashMap<>();
    private final List<StaticField> buffers = new ArrayList<>();

    private final Map<MethodInvocation, ASTNode> decompiledMethods = new HashMap<>();
    private final Map<MethodInvocationWithConstants, ASTNode> inlinedConstants = new HashMap<>();

    public ShaderCompiler(MalletContext globalContext, Type mainClass) {
        this.globalContext = globalContext;
        this.mainClass = mainClass;

        addDefaultState();
    }

    public ShaderCompiler(MalletContext globalContext, Class<?> mainClass) {
        this(globalContext, Type.getType(mainClass));
    }

    public Object2ObjectMap<PrimitiveConstant, String> getConstantNames() {
        return constantNames;
    }

    public Map<MethodInvocationWithConstants, CompiledMethod> getCompiledMethodMap() {
        return compiledMethodMap;
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

        CompiledMethod main = new CompiledMethod(astRoot, "main", Type.VOID_TYPE, new Type[0], false);
        Set<MethodInvocationWithConstants> methodInvocations = getAllMethodInvocations(main.getRoot());

        compileAll(methodInvocations);


        Set<Type> usedTypes = new HashSet<>();
        gatherUsedTypes(usedTypes, main);
        for (CompiledMethod compiledMethod : compiledMethodMap.values()) {
            gatherUsedTypes(usedTypes, compiledMethod);
        }
        usedTypes.removeIf(Objects::isNull);
        usedTypes = usedTypes.stream().map(t -> t.getSort() == Type.ARRAY ? t.getElementType() : t).collect(Collectors.toSet());

        for (Type type : usedTypes) {
            System.out.println("Used type: " + type);
        }

        StringBuilder glsl = new StringBuilder();
        glsl.append("#version 450 core\n\n");

        defineTypes(glsl, usedTypes);

        makeShaderIO(glsl);

        //Define constants
        glsl.append("\n");
        int i = 0;
        for (PrimitiveConstant constant : needFullConstants) {
            Type type = constant.getType();
            MalletType malletType = globalContext.getType(type);

            String name = "const_" + i++;
            constantNames.put(constant, name);
            glsl.append(malletType.getName()).append(" ");
            glsl.append(name).append(" = ");

            malletType.make(glsl, constant.obj(), globalContext);
            glsl.append(";\n");
        }
        glsl.append("\n");

        //Pre-declare all methods
        for (CompiledMethod compiledMethod : compiledMethodMap.values()) {
            compiledMethod.declare(glsl, globalContext);
        }

        glsl.append("\n");

        //Define all methods
        for (CompiledMethod compiledMethod : compiledMethodMap.values()) {
            compiledMethod.define(glsl, globalContext, this);
        }

        glsl.append("\n\n// MAIN\n\n");
        main.define(glsl, globalContext, this);

        return glsl.toString();
    }

    private void makeShaderIO(StringBuilder glsl) {
        Set<StaticField> shaderVertexInputs = new HashSet<>();
        Set<StaticField> shaderOutputs = new HashSet<>();
        Set<StaticField> shaderUniforms = new HashSet<>();
        Set<StaticField> shaderBuffers = new HashSet<>();

        for (StaticField staticField : globalState) {
            boolean isInput = staticField.hasAnnotation(In.class);
            boolean isOutput = staticField.hasAnnotation(Out.class);
            boolean isUniform = staticField.hasAnnotation(Uniform.class);
            boolean isBuffer = staticField.hasAnnotation(Buffer.class);

            //Make sure only one of these annotations is present
            int numAnnotations = 0;
            if (isInput) numAnnotations++;
            if (isOutput) numAnnotations++;
            if (isUniform) numAnnotations++;
            if (isBuffer) numAnnotations++;

            if (numAnnotations > 1) {
                throw new RuntimeException("Only one of the input type annotations can be present on a field");
            }

            if (isInput) {
                shaderVertexInputs.add(staticField);
            } else if (isOutput) {
                shaderOutputs.add(staticField);
            } else if (isUniform) {
                shaderUniforms.add(staticField);
            } else if (isBuffer) {
                shaderBuffers.add(staticField);
            }
        }

        //Uniforms
        for (StaticField staticField : shaderUniforms) {
            makeUniform(glsl, staticField);
        }

        glsl.append("\n");

        //Buffers
        for (StaticField staticField : shaderBuffers) {
            makeBuffer(glsl, staticField);
        }

        for (StaticField staticField : shaderVertexInputs) {
            MalletType malletType = globalContext.getType(staticField.getType());

            if(!malletType.isGLSLPrimitive()) {
                throw new RuntimeException("Vertex inputs must be primitive types");
            }

            makeInput(glsl, staticField, malletType);
        }

        for (StaticField staticField : shaderOutputs) {
            makeOutput(glsl, staticField);
        }

        //Compute shader specific
        Class<?> shaderClass = Util.getClass(mainClass);

        if (ComputeShader.class.isAssignableFrom(shaderClass)) {
            Layout layout = globalContext.getAnnotations(shaderClass).getAnnotation(Layout.class);
            if (layout == null) {
                throw new RuntimeException("Compute shader must have a layout annotation");
            }

            int x = layout.local_size_x();
            int y = layout.local_size_y();
            int z = layout.local_size_z();

            x = x == -1 ? 1 : x;
            y = y == -1 ? 1 : y;
            z = z == -1 ? 1 : z;

            glsl.append("layout(local_size_x = ").append(x).append(", local_size_y = ").append(y).append(", local_size_z = ").append(z).append(") in;\n");
        }
    }

    private void makeInput(StringBuilder glsl, StaticField staticField, MalletType malletType) {
        Layout layout = staticField.getAnnotation(Layout.class);

        if (layout != null) {
            MalletAnnotations.writeLayout(glsl, layout, false);
        }

        glsl.append("in ");

        glsl.append(malletType.getName());

        glsl.append(" ");

        glsl.append(staticField.getFieldName());

        glsl.append(";\n");
    }

    private void makeOutput(StringBuilder glsl, StaticField staticField) {
        Layout layout = staticField.getAnnotation(Layout.class);

        if (layout != null) {
            MalletAnnotations.writeLayout(glsl, layout, false);
        }

        glsl.append("out ");

        MalletType malletType = globalContext.getType(staticField.getType());

        glsl.append(malletType.getName());

        glsl.append(" ");

        glsl.append(staticField.getFieldName());

        glsl.append(";\n");
    }

    private void makeUniform(StringBuilder glsl, StaticField staticField) {
        Layout layout = staticField.getAnnotation(Layout.class);

        if (layout != null) {
            MalletAnnotations.writeLayout(glsl, layout, true);
        }

        glsl.append("uniform ");

        Type type = staticField.getType();
        Type elementType = type.getSort() == Type.ARRAY ? type.getElementType() : type;

        MalletType malletType = globalContext.getType(elementType);

        glsl.append(malletType.getName());
        glsl.append(" ");
        glsl.append(staticField.getFieldName());

        if (type.getSort() == Type.ARRAY) {
            writeArrayDimensions(glsl, staticField, type);
        }

        Object value = staticField.getValue();
        if (value != null) {
            glsl.append(" = ");
            malletType.make(glsl, staticField.getValue(), globalContext);
        }

        glsl.append(";\n");
    }

    private void makeBuffer(StringBuilder glsl, StaticField staticField) {
        Layout layout = staticField.getAnnotation(Layout.class);

        int id = buffers.size();
        buffers.add(staticField);

        if (layout == null) {
            glsl.append("layout(std430) ");
        } else {
            MalletAnnotations.writeLayout(glsl, layout, true);
        }

        glsl.append("buffer ");

        String structName = "BufferStruct_" + id;
        glsl.append(structName);

        glsl.append(" {\n");
        glsl.append("    ");

        Type type = staticField.getType();
        Type elementType = type.getSort() == Type.ARRAY ? type.getElementType() : type;

        MalletType malletType = globalContext.getType(elementType);

        glsl.append(malletType.getName());
        glsl.append(" value");

        if (type.getSort() == Type.ARRAY) {
            writeArrayDimensions(glsl, staticField, type);
        }

        glsl.append(";\n} ");

        glsl.append(staticField.getFieldName());

        glsl.append(";\n");
    }

    private void writeArrayDimensions(StringBuilder glsl, StaticField staticField, Type type) {
        ArraySize arraySize = staticField.getAnnotation(ArraySize.class);
        int numDimensions = type.getDimensions();

        int[] givenDimensions = arraySize != null ? arraySize.dimensions() : new int[0];

        if (givenDimensions.length + 1 == numDimensions) {
            glsl.append("[]");
        } else if (givenDimensions.length + 1 < numDimensions) {
            throw new RuntimeException("Not enough dimensions given for array");
        } else if (numDimensions < givenDimensions.length) {
            throw new RuntimeException("Too many dimensions given for array");
        }

        for (int givenDimension : givenDimensions) {
            glsl.append("[").append(givenDimension).append("]");
        }
    }

    private void defineTypes(StringBuilder glsl, Set<Type> usedTypes) {
        Set<Type> definedTypes = new HashSet<>();
        Set<Type> undefinedTypes = new HashSet<>(usedTypes);

        while (!undefinedTypes.isEmpty()) {
            for (Type type : undefinedTypes) {
                MalletType malletType = globalContext.getType(type);
                if (definedTypes.containsAll(malletType.dependsOn())) {
                    malletType.declareType(glsl, globalContext);
                    definedTypes.add(type);
                    undefinedTypes.remove(type);
                    break;
                }
            }
        }
    }

    private void gatherUsedTypes(Set<Type> usedTypes, CompiledMethod compiledMethod) {
        usedTypes.addAll(Arrays.asList(compiledMethod.getArgumentTypes()));
        if (compiledMethod.getReturnType() != Type.VOID_TYPE) {
            usedTypes.add(compiledMethod.getReturnType());
        }

        compiledMethod.getRoot().visitTree(node -> {
            if (node instanceof InstructionASTNode insnNode) {
                Instruction insn = insnNode.getInstruction();

                Ref<Function<Value, Value>> valueVisitor = new Ref<>(null);
                Function<Value, Value> valueVisitorFunc = (val) -> {
                    if (!(val instanceof Constant)) {
                        usedTypes.add(val.getType());
                    }
                    return val.copyValue(valueVisitor.value);
                };
                valueVisitor.value = valueVisitorFunc;

                insn.visitAndReplace(valueVisitorFunc, loc -> (Location) valueVisitorFunc.apply(loc));
            }
        });
    }

    private void compileAll(Set<MethodInvocationWithConstants> methodInvocations) {
        Queue<MethodInvocationWithConstants> queue = new ArrayDeque<>(methodInvocations);
        while(!queue.isEmpty()) {
            MethodInvocationWithConstants methodInvocation = queue.poll();
            if(compiledMethodMap.containsKey(methodInvocation)) {
                continue;
            }

            compileInvocation(methodInvocation, queue);
        }
    }

    private void compileInvocation(MethodInvocationWithConstants methodInvocation, Queue<MethodInvocationWithConstants> queue) {
        Type owner = methodInvocation.getMethodInvocation().methodOwner();
        if (methodInvocation.getMethodInvocation().type() != MethodInvocation.MethodCallType.STATIC && methodInvocation.getParamIndexToConstant().containsKey(0)) {
            owner = Type.getType(methodInvocation.getParamIndexToConstant().get(0).getClass());
        }

        if(AUTO_CONVERT.containsKey(owner)) {
            owner = AUTO_CONVERT.get(owner);
        }

        JavaDecompiler decompiler = makeDecompiler(
                owner,
                methodInvocation.getMethodInvocation().methodName(),
                methodInvocation.getMethodInvocation().methodDesc()
        );

        ASTNode astRoot = decompiler.decompile();

        //Convert params to var indices
        IntList paramIndices = new IntArrayList();
        Type[] baseType = methodInvocation.getMethodInvocation().methodDesc().getArgumentTypes();
        Type[] paramTypes;
        if(methodInvocation.getMethodInvocation().type() == MethodInvocation.MethodCallType.STATIC) {
            paramTypes = baseType;
        } else {
            paramTypes = new Type[baseType.length + 1];
            paramTypes[0] = owner;
            System.arraycopy(baseType, 0, paramTypes, 1, baseType.length);
        }

        int varIndex = 0;
        for(int i = 0; i < paramTypes.length; i++) {
            paramIndices.add(varIndex);
            varIndex += paramTypes[i].getSize();
        }

        //Inline constants
        Int2ObjectMap<Object> inlineArgs = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<Object> entry : methodInvocation.getParamIndexToConstant().int2ObjectEntrySet()) {
            inlineArgs.put(paramIndices.getInt(entry.getIntKey()), entry.getValue());
        }
        astRoot = inlineConstants(decompiler.getMethodNode(), astRoot, inlineArgs);

        //Inline method invocations

        List<Type> argTypes = new ArrayList<>();
        IntList argIndices = new IntArrayList();
        BooleanList outArgs = new BooleanArrayList();

        IntList mutatedArgs = this.getMutatedArgs(methodInvocation.getMethodInvocation());

        int argIndex = 0;
        for(int i = 0; i < paramTypes.length; i++) {
            if (!methodInvocation.getParamIndexToConstant().containsKey(i)) {
                argTypes.add(paramTypes[i]);
                argIndices.add(argIndex);
                outArgs.add(mutatedArgs.contains(i));
            }

            argIndex += paramTypes[i].getSize();
        }
        Type[] argTypesArray = argTypes.toArray(new Type[0]);

        String name = owner + "_" + methodInvocation.getMethodInvocation().methodName();
        CompiledMethod method = new CompiledMethod(astRoot, owner, methodInvocation.getMethodInvocation().methodName(), methodInvocation.getMethodInvocation().methodDesc().getReturnType(), methodInvocation.getActualTypes(), argIndices.toIntArray(), outArgs.toBooleanArray());
        compiledMethodMap.put(methodInvocation, method);

        Set<MethodInvocationWithConstants> methodInvocations = getAllMethodInvocations(method.getRoot());
        queue.addAll(methodInvocations);

        System.out.println("Compiled " + methodInvocation.getMethodInvocation().methodName());
    }

    private Set<MethodInvocationWithConstants> getAllMethodInvocations(ASTNode astRoot) {
        Set<MethodInvocationWithConstants> methodInvocations = new HashSet<>();
        getAllMethodInvocations(astRoot, methodInvocations);
        return methodInvocations;
    }

    private void getAllMethodInvocations(ASTNode astRoot, Set<MethodInvocationWithConstants> methodInvocations) {
        astRoot.visitTree(node -> {
            if (node instanceof InstructionASTNode insnAST) {
                if (insnAST.getInstruction() instanceof MethodCallInstruction call) {
                    methodInvocations.add(call.getMethodCall().toMethodInvocationWithConstants());
                }

                Ref<Function<Value, Value>> valueCopier = new Ref<>(null);
                Function<Value, Value> valueCopierFunc = val -> {
                    if (val instanceof MethodCallValue call) {
                        methodInvocations.add(call.getMethodCall().toMethodInvocationWithConstants());
                    }

                    return val.copyValue(valueCopier.value);
                };
                valueCopier.value = valueCopierFunc;

                insnAST.getInstruction().visitAndReplace(valueCopierFunc, (loc) -> (Location) loc.copyValue(valueCopier.value));
            }
        });
    }

    private ASTNode inlineConstants(MethodNode methodNode, ASTNode decompiled, Int2ObjectMap<Object> args) {
        ASTAnalysisResults<DefinedValue> definedValues = ASTAnalysis.analyseMethod(new DefinedSemilattice(), decompiled);

        Ref<ASTNode> curr = new Ref<>(null);

        return inlineConstants(definedValues, decompiled, args, curr);
    }

    private ASTNode inlineConstants(ASTAnalysisResults<DefinedValue> definedValues, ASTNode decompiled, Int2ObjectMap<Object> args, Ref<ASTNode> curr) {
        curr.value = decompiled;

        Function<Value, Value> valueCopier = (val) -> inlineValueConstant(val, definedValues, args, curr.value);

        return decompiled.visitAndReplace(
                node -> inlineConstants(definedValues, node, args, curr),
                insn -> {
                    if (insn instanceof AssignmentInstruction assign) {
                        if (assign.getValue() instanceof Constant cst) {
                            addFullConstant(cst);
                        }
                    }

                    return insn.visitAndReplace(
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
                if (!definedValues.in.get(insn).getDefinedVars().contains(var) && args.containsKey(index)) {
                    boolean primitive = var.getType().getSort() <= Type.DOUBLE;

                    constants.add(new PrimitiveConstant(args.get(index), primitive));
                    return new Constant(args.get(index), var, this);
                }
            }
        } else if (value instanceof StaticField field) {
            if (!globalState.contains(field)) {
                boolean primitive = field.getType().getSort() <= Type.DOUBLE;

                constants.add(new PrimitiveConstant(field.getValue(), primitive));
                return new Constant(field.getValue(), field, this);
            }
        } else if (value instanceof ObjectField fieldObj) {
            if (fieldObj.getObject() instanceof Constant cst) {
                boolean primitive = fieldObj.getType().getSort() <= Type.DOUBLE;

                Object val = fieldObj.getValue(cst.getValue());
                constants.add(new PrimitiveConstant(val, primitive));
                return new Constant(val, fieldObj, this);
            }
        } else if (value instanceof UnaryOperation unary) {
            if (unary.getArg() instanceof Constant cst) {
                Object arg = cst.getValue();

                if (unary.getOp() instanceof UnaryOperation.Op.InstanceofOp op) {
                    try {
                        Class<?> clazz = Class.forName(op.getType().getClassName());
                        return new Constant(clazz.isInstance(arg), unary, this);
                    }catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else if (unary.getOp() instanceof UnaryOperation.Op.CheckCastOp op) {
                    try {
                        Class<?> clazz = Class.forName(op.getType().getClassName());
                        if (clazz.isInstance(arg)) {
                            return new Constant(arg, unary, this);
                        } else {
                            throw new RuntimeException("Cannot cast " + arg + " to " + op.getType().getClassName());
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Type t = cst.getType();
                    boolean isPrimitive = t.getSort() <= Type.DOUBLE;

                    needFullConstants.add(new PrimitiveConstant(arg, isPrimitive));
                }
            }
        } else if (value instanceof BinaryOperation binOp) {
            if (binOp.getLeft() instanceof Constant cst) {
                Object left = cst.getValue();
                Type t = cst.getType();
                boolean isPrimitive = t.getSort() <= Type.DOUBLE;

                needFullConstants.add(new PrimitiveConstant(left, isPrimitive));
            }

            if (binOp.getRight() instanceof Constant cst) {
                Object right = cst.getValue();
                Type t = cst.getType();
                boolean isPrimitive = t.getSort() <= Type.DOUBLE;

                needFullConstants.add(new PrimitiveConstant(right, isPrimitive));
            }
        } else if (value instanceof ArrayElement array) {
            if (array.getArray() instanceof Constant cst) {
                Object arrayVal = cst.getValue();
                Type t = cst.getType();
                boolean isPrimitive = t.getSort() <= Type.DOUBLE;

                needFullConstants.add(new PrimitiveConstant(arrayVal, isPrimitive));
            }
        } else if (value instanceof CopyValue copyValue) {
            if (copyValue.getValue() instanceof Constant cst) {
                Object val = cst.getValue();
                Type t = cst.getType();
                boolean isPrimitive = t.getSort() <= Type.DOUBLE;

                needFullConstants.add(new PrimitiveConstant(val, isPrimitive));
            }
        }

        return value;
    }

    private void addFullConstant(Constant constant) {
        Object value = constant.getValue();
        Type type = constant.getType();
        boolean isPrimitive = type.getSort() <= Type.DOUBLE;

        needFullConstants.add(new PrimitiveConstant(value, isPrimitive));
    }

    private void addDefaultState() {
        Class<? extends Shader> shaderClass = (Class<? extends Shader>) Util.getClass(mainClass).getSuperclass();

        Type shaderType = Type.getType(shaderClass);

        for (Field field : shaderClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ShaderVar.class)) {
                Type fieldType = Type.getType(field.getType());

                globalState.add(
                        new StaticField(
                                shaderType,
                                field.getName(),
                                fieldType,
                                this
                        )
                );

                globalState.add(
                        new StaticField(
                                mainClass,
                                field.getName(),
                                fieldType,
                                this
                        )
                );
            }
        }
    }

    private JavaDecompiler makeMainDecompiler() {
        ClassNode mainClassNode = globalContext.findClass(mainClass);

        //Find global state
        for(FieldNode fieldNode : mainClassNode.fields) {
            if(ASMUtil.isStatic(fieldNode.access)) {
                globalState.add(new StaticField(mainClass, fieldNode.name, Type.getType(fieldNode.desc), this));
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

    public void callMethod(StringBuilder sb, MethodCall methodCall) {
        //Remove object.<init>
        if(methodCall.getInvocation().methodName().equals("<init>") && methodCall.getInvocation().methodDesc().getDescriptor().equals("()V")) {
            if (methodCall.getInvocation().methodOwner().equals(Type.getType(Object.class))) {
                sb.append("//Removed Object <init>()");
            }
        }

        MethodInvocationWithConstants invocation = methodCall.toMethodInvocationWithConstants();
        CompiledMethod method = this.getCompiledMethodMap().get(invocation);

        if (method != null) {
            sb.append(method.getName());
            sb.append("(");

            boolean addComma = false;
            int i = -1;
            for (Value arg : methodCall.getArgs()) {
                i++;
                if (invocation.getParamIndexToConstant().containsKey(i)) continue;

                if (addComma) {
                    sb.append(", ");
                }
                addComma = true;

                arg.writeGLSL(sb, globalContext, this);
            }
            sb.append(")");

            return;
        }

        //TODO: Method Resolvers
        throw new UnsupportedOperationException("Method Resolvers noNMet implemented yet. Could not find " + methodCall);
    }

    public void getStatic(StringBuilder sb, StaticField field) {
        if (!globalState.contains(field)) {
            throw new IllegalStateException("Static field " + field + " not found in global state");
        }

        ShaderVar in = field.getAnnotation(ShaderVar.class);
        if (in != null && !in.getter().equals("")) {
            sb.append(in.getter());
            return;
        }

        sb.append(field.getFieldName());
        if (buffers.contains(field)) {
            sb.append(".value");
        }
    }

    public void getField(StringBuilder sb, ObjectField field) {
        Type type = field.getObject().getType();
        MalletType malletType = globalContext.getType(type);

        malletType.writeGLSLForGetField(sb, field, this);
    }

    public MalletContext getGlobalContext() {
        return globalContext;
    }

    static {
        if(AUTO_CONVERT_JOML_ITF) {
            //Float
            AUTO_CONVERT.put(Type.getType(Vector2fc.class), Type.getType(Vector2f.class));
            AUTO_CONVERT.put(Type.getType(Vector3fc.class), Type.getType(Vector3f.class));
            AUTO_CONVERT.put(Type.getType(Vector4fc.class), Type.getType(Vector4f.class));
            AUTO_CONVERT.put(Type.getType(Matrix2fc.class), Type.getType(Matrix2f.class));
            AUTO_CONVERT.put(Type.getType(Matrix3fc.class), Type.getType(Matrix3f.class));
            AUTO_CONVERT.put(Type.getType(Matrix3x2fc.class), Type.getType(Matrix3x2f.class));
            AUTO_CONVERT.put(Type.getType(Matrix4fc.class), Type.getType(Matrix4f.class));
            AUTO_CONVERT.put(Type.getType(Matrix4x3fc.class), Type.getType(Matrix4x3f.class));

            //Double
            AUTO_CONVERT.put(Type.getType(Vector2dc.class), Type.getType(Vector2d.class));
            AUTO_CONVERT.put(Type.getType(Vector3dc.class), Type.getType(Vector3d.class));
            AUTO_CONVERT.put(Type.getType(Vector4dc.class), Type.getType(Vector4d.class));
            AUTO_CONVERT.put(Type.getType(Matrix2dc.class), Type.getType(Matrix2d.class));
            AUTO_CONVERT.put(Type.getType(Matrix3dc.class), Type.getType(Matrix3d.class));
            AUTO_CONVERT.put(Type.getType(Matrix3x2dc.class), Type.getType(Matrix3x2d.class));
            AUTO_CONVERT.put(Type.getType(Matrix4dc.class), Type.getType(Matrix4d.class));
            AUTO_CONVERT.put(Type.getType(Matrix4x3dc.class), Type.getType(Matrix4x3d.class));

            //Int
            AUTO_CONVERT.put(Type.getType(Vector2ic.class), Type.getType(Vector2i.class));
            AUTO_CONVERT.put(Type.getType(Vector3ic.class), Type.getType(Vector3i.class));
            AUTO_CONVERT.put(Type.getType(Vector4ic.class), Type.getType(Vector4i.class));
        }
    }
}
