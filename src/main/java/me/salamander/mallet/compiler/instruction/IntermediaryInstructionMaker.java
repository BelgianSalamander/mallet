package me.salamander.mallet.compiler.instruction;

import me.salamander.mallet.compiler.JavaDecompiler;
import me.salamander.mallet.compiler.instruction.value.*;
import me.salamander.mallet.util.ASMUtil;
import me.salamander.mallet.util.MethodCall;
import me.salamander.mallet.util.MethodInvocation;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.objectweb.asm.Opcodes.*;

public class IntermediaryInstructionMaker {
    private final Stack<Type> stack = new Stack<>();
    private final Type[] localVars;
    private final List<Instruction> instructions = new ArrayList<>();
    private final JavaDecompiler decompiler;

    public IntermediaryInstructionMaker(Frame<? extends BasicValue> frame, JavaDecompiler decompiler) {
        this.localVars = new Type[frame.getLocals()];
        this.decompiler = decompiler;

        for(int i = 0; i < frame.getLocals(); i++){
            localVars[i] = frame.getLocal(i).getType();
        }

        for(int i = 0; i < frame.getStackSize(); i++){
            stack.push(frame.getStack(i).getType());
        }
    }

    public void interpret(AbstractInsnNode insn){
        switch (insn.getOpcode()){
            case NOP -> {}
            case ACONST_NULL -> addInstruction(
                    new AssignmentInstruction(
                            push(BasicVerifier.NULL_TYPE),
                            new LiteralValue(BasicVerifier.NULL_TYPE, null)
                    )
            );
            case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> addInstruction(
                    new AssignmentInstruction(
                            push(Type.INT_TYPE),
                            new LiteralValue(Type.INT_TYPE, insn.getOpcode() - ICONST_0)
                    )
            );
            case LCONST_0, LCONST_1 -> addInstruction(
                    new AssignmentInstruction(
                            push(Type.LONG_TYPE),
                            new LiteralValue(Type.LONG_TYPE, (long) (insn.getOpcode() - LCONST_0))
                    )
            );
            case FCONST_0, FCONST_1, FCONST_2 -> addInstruction(
                    new AssignmentInstruction(
                            push(Type.FLOAT_TYPE),
                            new LiteralValue(Type.FLOAT_TYPE, (float) (insn.getOpcode() - FCONST_0))
                    )
            );
            case DCONST_0, DCONST_1 -> addInstruction(
                    new AssignmentInstruction(
                            push(Type.DOUBLE_TYPE),
                            new LiteralValue(Type.DOUBLE_TYPE, (double) (insn.getOpcode() - DCONST_0))
                    )
            );
            case SIPUSH, BIPUSH -> addInstruction(
                    new AssignmentInstruction(
                            push(Type.INT_TYPE),
                            new LiteralValue(Type.INT_TYPE, ((IntInsnNode) insn).operand)
                    )
            );
            case LDC -> {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                Type type;
                if(ldc.cst instanceof Integer){
                    type = Type.INT_TYPE;
                }else if(ldc.cst instanceof Float){
                    type = Type.FLOAT_TYPE;
                }else if(ldc.cst instanceof Long){
                    type = Type.LONG_TYPE;
                }else if(ldc.cst instanceof Double){
                    type = Type.DOUBLE_TYPE;
                }else if(ldc.cst instanceof String){
                    type = Type.getType(String.class);
                }else {
                    throw new RuntimeException("Unknown constant type: " + ldc.cst.getClass());
                }

                addInstruction(
                        new AssignmentInstruction(
                                push(type),
                                new LiteralValue(type, ldc.cst)
                        )
                );
            }
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                int index = ((VarInsnNode) insn).var;
                Value local = getLocal(index);
                addInstruction(
                        new AssignmentInstruction(
                                push(local.getType()),
                                local
                        )
                );
            }
            case IALOAD, LALOAD, FALOAD, DALOAD, BALOAD, CALOAD, SALOAD, AALOAD -> {
                Value index = pop();
                Value array = pop();
                Location save = push(ASMUtil.getSingleType(array.getType()));
                addInstruction(
                        new AssignmentInstruction(
                                save,
                                new ArrayElement(array, index)
                        )
                );
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                int index = ((VarInsnNode) insn).var;
                Value value = pop();
                addInstruction(
                        new AssignmentInstruction(
                                getLocal(index),
                                value
                        )
                );
            }
            case IASTORE, LASTORE, FASTORE, DASTORE, BASTORE, CASTORE, SASTORE, AASTORE -> {
                Value value = pop();
                Value index = pop();
                Value array = pop();
                addInstruction(
                        new AssignmentInstruction(
                                new ArrayElement(array, index),
                                value
                        )
                );
            }
            case POP, POP2 -> {}
            case DUP -> {
                makeDups(
                        1, /* = */ 0
                );
            }
            case DUP_X1, SWAP -> {
                //SWAP does a similar thing to DUP_X1

                /*
                 * stack_+1 = stack_0
                 * stack_0 = stack_-1
                 * stack_-1 = stack_+1
                 */

                makeDups(
                        1, /* = */ 0,
                        0, /* = */ -1
                        -1, /* = */ 1
                );
            }
            case DUP_X2 -> {
                Type value2Type = peekType(1);

                if(value2Type.getSize() == 2) {
                    makeDups(
                            1, /* = */ 0,
                            0, /* = */ -1
                                    -1, /* = */ 1
                    );
                }else{
                    makeDups(
                            1, /* = */ 0,
                            0, /* = */ -1,
                            -1, /* = */ 1,
                            -2, /* = */ 1
                    );
                }
            }
            case DUP2 -> {
                Type value1Type = peekType(0);

                if(value1Type.getSize() == 2) {
                    makeDups(
                            1, /* = */ 0
                    );
                }else{
                    makeDups(
                            1, /* = */ -1,
                            2, /* = */ 0
                    );
                }
            }
            case DUP2_X1 -> {
                Type value1Type = peekType(0);

                if(value1Type.getSize() == 2) {
                    makeDups(
                            1, /* = */ 0,
                            0, /* = */ -1
                            -1, /* = */ 1
                    );
                }else{
                    makeDups(
                            2, /* = */ 0,
                            1, /* = */ -1,
                            0, /* = */ -2,
                            -1, /* = */ 2,
                            -2, /* = */ 1
                    );
                }
            }
            case DUP2_X2 -> {
                Type value1Type = peekType(0);
                Type value2Type = peekType(1);

                if(value1Type.getSize() == 2 && value2Type.getSize() == 2) {
                    //Form 4

                    makeDups(
                            1, /* = */ 0,
                            0, /* = */ -1
                            -1, /* = */ 1
                    );
                }else if(value1Type.getSize() == 2 && value2Type.getSize() == 1) {
                    //Form 2

                    makeDups(
                            1, /* = */ 0,
                            0, /* = */ -1,
                            -1, /* = */ -2,
                            -2, /* = */ 1
                    );
                }else {
                    Type value3Type = peekType(2);

                    if(value3Type.getSize() == 2) {
                        //Form 3

                        makeDups(
                                2, /* = */ 0,
                                1, /* = */ -1,
                                0, /* = */ -2,
                                -1, /* = */ 2,
                                -2, /* = */ 1
                        );
                    }else{
                        //Form 1

                        makeDups(
                                2, /* = */ 0,
                                1, /* = */ -1,
                                0, /* = */ -2,
                                -1, /* = */ -3,
                                -2, /* = */ 2,
                                -3, /* = */ 1
                        );
                    }
                }
            }
            case IADD, LADD, FADD, DADD -> binaryOp(BinaryOperation.Op.ADD);
            case ISUB, LSUB, FSUB, DSUB -> binaryOp(BinaryOperation.Op.SUB);
            case IMUL, LMUL, FMUL, DMUL -> binaryOp(BinaryOperation.Op.MUL);
            case IDIV, LDIV, FDIV, DDIV -> binaryOp(BinaryOperation.Op.DIV);
            case IREM, LREM, FREM, DREM -> binaryOp(BinaryOperation.Op.REM);
            case INEG, LNEG, FNEG, DNEG -> unaryOp(UnaryOperation.Op.NEG);
            case ISHL, LSHL -> binaryOp(BinaryOperation.Op.SHL);
            case ISHR, LSHR -> binaryOp(BinaryOperation.Op.SHR);
            case IUSHR, LUSHR -> binaryOp(BinaryOperation.Op.USHR);
            case IAND, LAND -> binaryOp(BinaryOperation.Op.AND);
            case IOR, LOR -> binaryOp(BinaryOperation.Op.OR);
            case IXOR, LXOR -> binaryOp(BinaryOperation.Op.XOR);
            case IINC -> {
                IincInsnNode iinc = (IincInsnNode) insn;
                Variable variable = getLocal(iinc.var);

                instructions.add(new AssignmentInstruction(
                        variable,
                        new BinaryOperation(
                                variable,
                                new LiteralValue(Type.INT_TYPE, iinc.incr),
                                BinaryOperation.Op.ADD
                        )
                ));
            }
            case I2L, F2L, D2L -> unaryOp(UnaryOperation.Op.TO_LONG);
            case I2F, L2F, D2F -> unaryOp(UnaryOperation.Op.TO_FLOAT);
            case I2D, L2D, F2D -> unaryOp(UnaryOperation.Op.TO_DOUBLE);
            case L2I, F2I, D2I -> unaryOp(UnaryOperation.Op.TO_INT);
            case I2B -> trimInt(1);
            case I2C, I2S -> trimInt(2);
            case LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> binaryOp(BinaryOperation.Op.CMP);
            case IFEQ -> compareZeroAndJump(BinaryOperation.Op.EQ, ((JumpInsnNode) insn).label);
            case IFNE -> compareZeroAndJump(BinaryOperation.Op.NE, ((JumpInsnNode) insn).label);
            case IFLT -> compareZeroAndJump(BinaryOperation.Op.LT, ((JumpInsnNode) insn).label);
            case IFGE -> compareZeroAndJump(BinaryOperation.Op.GE, ((JumpInsnNode) insn).label);
            case IFGT -> compareZeroAndJump(BinaryOperation.Op.GT, ((JumpInsnNode) insn).label);
            case IFLE -> compareZeroAndJump(BinaryOperation.Op.LE, ((JumpInsnNode) insn).label);
            case IF_ICMPEQ, IF_ACMPEQ -> compareAndJump(BinaryOperation.Op.EQ, ((JumpInsnNode) insn).label);
            case IF_ICMPNE, IF_ACMPNE -> compareAndJump(BinaryOperation.Op.NE, ((JumpInsnNode) insn).label);
            case IF_ICMPLT -> compareAndJump(BinaryOperation.Op.LT, ((JumpInsnNode) insn).label);
            case IF_ICMPGE -> compareAndJump(BinaryOperation.Op.GE, ((JumpInsnNode) insn).label);
            case IF_ICMPGT -> compareAndJump(BinaryOperation.Op.GT, ((JumpInsnNode) insn).label);
            case IF_ICMPLE -> compareAndJump(BinaryOperation.Op.LE, ((JumpInsnNode) insn).label);
            case GOTO -> addInstruction(new GotoInstruction(decompiler.getLabel(((JumpInsnNode) insn).label)));
            case JSR -> {throw new UnsupportedOperationException("JSR is not supported");}
            case RET -> {throw new UnsupportedOperationException("RET is not supported");}
            case TABLESWITCH -> {
                TableSwitchInsnNode tableswitch = (TableSwitchInsnNode) insn;

                List<Label> labels = tableswitch.labels.stream()
                        .map(decompiler::getLabel)
                        .toList();

                SwitchInstruction switchInstruction = new SwitchInstruction(pop());

                switchInstruction.addBranches(tableswitch.min, tableswitch.max, labels);

                if(tableswitch.dflt != null) {
                    switchInstruction.setDefaultBranch(decompiler.getLabel(tableswitch.dflt));
                }

                addInstruction(switchInstruction);
            }
            case LOOKUPSWITCH -> {
                LookupSwitchInsnNode lookupswitch = (LookupSwitchInsnNode) insn;

                List<Label> labels = lookupswitch.labels.stream()
                        .map(decompiler::getLabel)
                        .toList();

                SwitchInstruction switchInstruction = new SwitchInstruction(pop());

                for (int i = 0; i < labels.size(); i++) {
                    switchInstruction.addBranch(lookupswitch.keys.get(i), labels.get(i));
                }

                if(lookupswitch.dflt != null) {
                    switchInstruction.setDefaultBranch(decompiler.getLabel(lookupswitch.dflt));
                }

                addInstruction(switchInstruction);
            }
            case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> addInstruction(new ReturnInstruction(pop()));
            case RETURN -> addInstruction(new ReturnInstruction(null));
            case GETSTATIC -> {
                FieldInsnNode node = (FieldInsnNode) insn;
                Type type = Type.getType(node.desc);
                addInstruction(
                        new AssignmentInstruction(
                                push(type),
                                new StaticField(
                                        Type.getObjectType(node.owner),
                                        node.name,
                                        type
                                )
                        )
                );
            }
            case PUTSTATIC -> {
                FieldInsnNode node = (FieldInsnNode) insn;
                Type type = Type.getType(node.desc);
                addInstruction(
                        new AssignmentInstruction(
                                new StaticField(
                                        Type.getObjectType(node.owner),
                                        node.name,
                                        type
                                ),
                                pop()
                        )
                );
            }
            case GETFIELD -> {
                FieldInsnNode node = (FieldInsnNode) insn;
                Type type = Type.getType(node.desc);
                Value obj = pop();
                addInstruction(
                        new AssignmentInstruction(
                                push(type),
                                new ObjectField(
                                        obj,
                                        Type.getObjectType(node.owner),
                                        node.name,
                                        type
                                )
                        )
                );
            }
            case PUTFIELD -> {
                FieldInsnNode node = (FieldInsnNode) insn;
                Type type = Type.getType(node.desc);
                Value value = pop();
                Value obj = pop();
                addInstruction(
                        new AssignmentInstruction(
                                new ObjectField(
                                        obj,
                                        Type.getObjectType(node.owner),
                                        node.name,
                                        type
                                ),
                                value
                        )
                );
            }
            case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE -> {
                MethodInvocation invocation = MethodInvocation.of((MethodInsnNode) insn);

                if(invocation.isCopyMethod()) {
                    Value arg = pop();
                    Location location = push(arg.getType());
                    addInstruction(new AssignmentInstruction(
                            location,
                            new CopyValue(arg)
                    ));

                    return;
                }

                Value[] args = new Value[invocation.getArgumentTypes().length];

                for (int i = args.length - 1; i >= 0; i--) {
                    args[i] = pop();
                }

                MethodCall methodCall = new MethodCall(invocation, args);

                if(invocation.returnsValue()) {
                    addInstruction(
                            new AssignmentInstruction(
                                    push(invocation.getReturnType()),
                                    new MethodCallValue(methodCall, decompiler.getShaderCompiler())
                            )
                    );
                }else{
                    addInstruction(new MethodCallInstruction(methodCall));
                }
            }
            case INVOKEDYNAMIC -> {throw new UnsupportedOperationException("InvokeDynamic is not supported");}
            case NEW -> {
                TypeInsnNode node = (TypeInsnNode) insn;
                Type type = Type.getObjectType(node.desc);
                addInstruction(
                        new AssignmentInstruction(
                                push(type),
                                new NewValue(type)
                        )
                );
            }
            case NEWARRAY -> {
                IntInsnNode node = (IntInsnNode) insn;

                Type type = switch (node.operand) {
                    case T_BOOLEAN -> Type.BOOLEAN_TYPE;
                    case T_CHAR -> Type.CHAR_TYPE;
                    case T_BYTE -> Type.BYTE_TYPE;
                    case T_SHORT -> Type.SHORT_TYPE;
                    case T_INT -> Type.INT_TYPE;
                    case T_FLOAT -> Type.FLOAT_TYPE;
                    case T_DOUBLE -> Type.DOUBLE_TYPE;
                    case T_LONG -> Type.LONG_TYPE;
                    default -> throw new IllegalArgumentException("Unknown type: " + node.operand);
                };

                Value size = pop();
                NewArray newArray = new NewArray(type, size);
                addInstruction(
                        new AssignmentInstruction(
                                push(newArray.getType()),
                                newArray
                        )
                );
            }
            case ANEWARRAY -> {
                TypeInsnNode node = (TypeInsnNode) insn;

                Type type = Type.getType(node.desc);
                NewArray newArray = new NewArray(type, pop());
                addInstruction(
                        new AssignmentInstruction(
                                push(newArray.getType()),
                                newArray
                        )
                );
            }
            case MULTIANEWARRAY -> {
                MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) insn;

                Value[] dimensions = new Value[node.dims];

                for (int i = dimensions.length - 1; i >= 0; i--) {
                    dimensions[i] = pop();
                }

                Type type = Type.getType(node.desc);
                NewArray newArray = new NewArray(type, dimensions);

                addInstruction(
                        new AssignmentInstruction(
                                push(newArray.getType()),
                                newArray
                        )
                );
            }
            case ARRAYLENGTH -> unaryOp(UnaryOperation.Op.ARRAY_LENGTH);
            case ATHROW -> throw new UnsupportedOperationException("ATHROW is not supported");
            case CHECKCAST -> {
                TypeInsnNode node = (TypeInsnNode) insn;
                if(!node.desc.equals(pop().getType().getDescriptor())) {
                    //throw new UnsupportedOperationException("Not implemented yet");
                    System.err.println("IGNORING CHECKCAST. Not Implemented yet");
                }

                //unaryOp(UnaryOperation.Op.makeCheckCast(Type.getObjectType(((TypeInsnNode) insn).desc)));
            }
            case INSTANCEOF -> {
                throw new UnsupportedOperationException("INSTANCEOF is not supported yet");
                //unaryOp(UnaryOperation.Op.makeInstanceOf(Type.getObjectType(((TypeInsnNode) insn).desc)));
            }
            case MONITORENTER -> throw new UnsupportedOperationException("MONITORENTER is not supported");
            case MONITOREXIT -> throw new UnsupportedOperationException("MONITOREXIT is not supported");
            case IFNULL, IFNONNULL -> {
                JumpInsnNode node = (JumpInsnNode) insn;
                Label target = decompiler.getLabel(node.label);

                Value value = pop();

                UnaryOperation.Op op = node.getOpcode() == IFNULL ? UnaryOperation.Op.ISNULL : UnaryOperation.Op.ISNOTNULL;

                instructions.add(
                        new JumpIfInstruction(
                                new UnaryOperation(value, op),
                                target
                        )
                );
            }

            case -1 -> {
                if (insn instanceof LabelNode labelNode){
                    Label label = decompiler.getLabel(labelNode);
                    addInstruction(new LabelInstruction(label));
                }
            }

            default -> System.err.println("Unhandled opcode: " + insn.getOpcode());
        }
    }

    private void compareAndJump(BinaryOperation.Op eq, LabelNode label) {
        Value value1 = pop();
        Value value2 = pop();

        instructions.add(
                new JumpIfInstruction(
                        new BinaryOperation(value1, value2, eq),
                        decompiler.getLabel(label)
                )
        );
    }

    private void compareZeroAndJump(BinaryOperation.Op op, LabelNode labelNode) {
        Value value = pop();

        instructions.add(
                new JumpIfInstruction(
                        new BinaryOperation(
                                value,
                                new LiteralValue(value.getType(), 0),
                                op
                        ),
                        decompiler.getLabel(labelNode)
                )
        );
    }

    private void trimInt(int bytes) {
        Value value = pop();
        Location save = push(Type.INT_TYPE);

        instructions.add(
                new AssignmentInstruction(
                        save,
                        new BinaryOperation(
                                value,
                                new LiteralValue(Type.INT_TYPE, 1 << (bytes * 8)),
                                BinaryOperation.Op.AND
                        )
                )
        );
    }

    private void binaryOp(BinaryOperation.Op op) {
        Value right = pop();
        Value left = pop();
        Location save = push(op.getResultingType(left.getType(), right.getType()));

        addInstruction(
                new AssignmentInstruction(
                        save,
                        new BinaryOperation(
                                left,
                                right,
                                op
                        )
                )
        );
    }

    private void unaryOp(UnaryOperation.Op op) {
        Value value = pop();
        Location save = push(op.getResultingType(value.getType()));

        addInstruction(
                new AssignmentInstruction(
                        save,
                        new UnaryOperation(
                                value,
                                op
                        )
                )
        );
    }

    private void makeDups(int... assignments) {
        //0 is top of stack, -1 is second from top, +1 is on top of stack

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(int i : assignments){
            min = Math.min(min, i);
            max = Math.max(max, i);
        }

        int stackTop = stack.size() - 1;

        Type[] types = new Type[max - min + 1];
        for(int i = min; i <= 0; i++){
            types[i - min] = stack.get(stackTop + i);
        }

        for(int i = 0; i < assignments.length; i += 2) {
            int index = assignments[i];
            int value = assignments[i + 1];

            Type valueType = types[value - min];
            addInstruction(
                    new AssignmentInstruction(
                            new Variable(valueType, index + stackTop, VariableType.STACK),
                            new Variable(valueType, value + stackTop, VariableType.STACK)
                    )
            );

            types[index - min] = valueType;
        }
    }

    private Variable getLocal(int var) {
        return new Variable(localVars[var], var, VariableType.LOCAL);
    }

    public List<Instruction> getInstructions(){
        return instructions;
    }

    private void addInstruction(Instruction instruction){
        instructions.add(instruction);
    }

    private Value pop(){
        int index = stack.size() - 1;
        return new Variable(stack.pop(), index, VariableType.STACK);
    }

    private Value peek(){
        int index = stack.size() - 1;
        return new Variable(stack.peek(), index, VariableType.STACK);
    }

    private Location peekLocation(Type type){
        int index = stack.size() - 1;
        return new Variable(type, index, VariableType.STACK);
    }

    //Used for simulation of stack when chaining instructions for a single op
    private Location push(Type type){
        stack.push(type);
        return new Variable(type, stack.size() - 1, VariableType.STACK);
    }

    private Type peekType(int depth){
        return stack.get(stack.size() - 1 - depth);
    }
}
