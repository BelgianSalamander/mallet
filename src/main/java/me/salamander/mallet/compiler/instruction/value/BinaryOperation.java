package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BinaryOperation implements Value {
    private Value left;
    private Value right;
    private Op op;

    public BinaryOperation(Value left, Value right, Op op) {
        this.left = left;
        this.right = right;
        this.op = op;

        checkTypes();
    }

    private void checkTypes(){
        if(!op.checkTypes(left.getType(), right.getType())){
            throw new IllegalStateException("Invalid types for binary operation");
        }
    }

    public Value getLeft() {
        return left;
    }

    public Value getRight() {
        return right;
    }

    public Op getOp() {
        return op;
    }

    public void setLeft(Value left) {
        this.left = left;
        checkTypes();
    }

    public void setRight(Value right) {
        this.right = right;
        checkTypes();
    }

    public void setOp(Op op) {
        this.op = op;
        checkTypes();
    }

    @Override
    public String toString() {
        return left + " " + op + " " + right;
    }

    @Override
    public Type getType() {
        return op.getResultingType(left.getType(), right.getType());
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return left.isInvalidatedByChangeIn(value) || right.isInvalidatedByChangeIn(value);
    }

    @Override
    public List<Variable> usedVariables() {
        List<Variable> used = new ArrayList<>(left.usedVariables());
        used.addAll(right.usedVariables());
        return used;
    }

    @Override
    public boolean allowInline() {
        return left.allowInline() && right.allowInline();
    }

    @Override
    public boolean allowDuplicateInline() {
        return left.allowDuplicateInline() && right.allowDuplicateInline();
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new BinaryOperation(innerValueCopier.apply(left), innerValueCopier.apply(right), op);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    @Override
    public @Nullable Value trySimplify() {
        boolean changed = false;

        Value leftSimplified = left.trySimplify();
        if(leftSimplified != null){
            left = leftSimplified;
            changed = true;
        }

        Value rightSimplified = right.trySimplify();
        if(rightSimplified != null){
            right = rightSimplified;
            changed = true;
        }

        if (this.op instanceof ComparisonOp compareOp) {
            if (left instanceof BinaryOperation leftBin && leftBin.op == Op.CMP && right instanceof LiteralValue literal && literal.getType() == Type.INT_TYPE && ((int) literal.getValue()) == 0) {
                return new BinaryOperation(leftBin.left, leftBin.right, compareOp);
            } else if (right instanceof BinaryOperation rightBin && rightBin.op == Op.CMP && left instanceof LiteralValue literal2 && literal2.getType() == Type.INT_TYPE && ((int) literal2.getValue()) == 0) {
                return new BinaryOperation(rightBin.left, rightBin.right, compareOp);
            }
        }

        if(changed){
            checkTypes();
            return this;
        } else {
            return null;
        }
    }

    public interface Op{
        Op ADD = new NumberOp("+") {
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if(leftNum instanceof Double || rightNum instanceof Double){
                    return leftNum.doubleValue() + rightNum.doubleValue();
                } else if (leftNum instanceof Float || rightNum instanceof Float){
                    return leftNum.floatValue() + rightNum.floatValue();
                } else if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() + rightNum.longValue();
                } else {
                    return leftNum.intValue() + rightNum.intValue();
                }
            }
        };
        Op SUB = new NumberOp("-"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if(leftNum instanceof Double || rightNum instanceof Double){
                    return leftNum.doubleValue() - rightNum.doubleValue();
                } else if (leftNum instanceof Float || rightNum instanceof Float){
                    return leftNum.floatValue() - rightNum.floatValue();
                } else if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() - rightNum.longValue();
                } else {
                    return leftNum.intValue() - rightNum.intValue();
                }
            }
        };
        Op MUL = new NumberOp("*") {
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if(leftNum instanceof Double || rightNum instanceof Double){
                    return leftNum.doubleValue() * rightNum.doubleValue();
                } else if (leftNum instanceof Float || rightNum instanceof Float){
                    return leftNum.floatValue() * rightNum.floatValue();
                } else if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() * rightNum.longValue();
                } else {
                    return leftNum.intValue() * rightNum.intValue();
                }
            }
        };
        Op DIV = new NumberOp("/"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if(leftNum instanceof Double || rightNum instanceof Double){
                    return leftNum.doubleValue() / rightNum.doubleValue();
                } else if (leftNum instanceof Float || rightNum instanceof Float){
                    return leftNum.floatValue() / rightNum.floatValue();
                } else if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() / rightNum.longValue();
                } else {
                    return leftNum.intValue() / rightNum.intValue();
                }
            }
        };
        Op REM = new NumberOp("%"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if(leftNum instanceof Double || rightNum instanceof Double){
                    return leftNum.doubleValue() % rightNum.doubleValue();
                } else if (leftNum instanceof Float || rightNum instanceof Float){
                    return leftNum.floatValue() % rightNum.floatValue();
                } else if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() % rightNum.longValue();
                } else {
                    return leftNum.intValue() % rightNum.intValue();
                }
            }
        };
        Op AND = new NumberOp("&"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() & rightNum.longValue();
                } else {
                    return leftNum.intValue() & rightNum.intValue();
                }
            }
        };
        Op OR = new NumberOp("|"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() | rightNum.longValue();
                } else {
                    return leftNum.intValue() | rightNum.intValue();
                }
            }
        };
        Op XOR = new NumberOp("^"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() ^ rightNum.longValue();
                } else {
                    return leftNum.intValue() ^ rightNum.intValue();
                }
            }
        };
        Op SHL = new NumberOp("<<"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() << rightNum.longValue();
                } else {
                    return leftNum.intValue() << rightNum.intValue();
                }
            }
        };
        Op SHR = new NumberOp(">>"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() >> rightNum.longValue();
                } else {
                    return leftNum.intValue() >> rightNum.intValue();
                }
            }
        };
        Op USHR = new NumberOp(">>>") {
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long){
                    return leftNum.longValue() >>> rightNum.longValue();
                } else {
                    return leftNum.intValue() >>> rightNum.intValue();
                }
            }
        };

        Op CMP = new Op() {
            @Override
            public boolean checkTypes(Type left, Type right) {
                return left.equals(right);
            }

            @Override
            public Type getResultingType(Type left, Type right) {
                return Type.INT_TYPE;
            }

            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                if (leftNum instanceof Long || rightNum instanceof Long) {
                    return Long.compare(leftNum.longValue(), rightNum.longValue());
                } else if (leftNum instanceof Float || rightNum instanceof Float) {
                    return Float.compare(leftNum.floatValue(), rightNum.floatValue());
                } else if (leftNum instanceof Double || rightNum instanceof Double) {
                    return Double.compare(leftNum.doubleValue(), rightNum.doubleValue());
                } else {
                    return Integer.compare(leftNum.intValue(), rightNum.intValue());
                }
            }

            @Override
            public String toString() {
                return "cmp";
            }
        };

        Op LT = new ComparisonOp("<"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                return leftNum.doubleValue() < rightNum.doubleValue();
            }
        };
        Op LE = new ComparisonOp("<="){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                return leftNum.doubleValue() <= rightNum.doubleValue();
            }
        };
        Op GT = new ComparisonOp(">"){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                return leftNum.doubleValue() > rightNum.doubleValue();
            }
        };
        Op GE = new ComparisonOp(">="){
            @Override
            public Object apply(Object left, Object right) {
                Number leftNum = (Number) left;
                Number rightNum = (Number) right;

                return leftNum.doubleValue() >= rightNum.doubleValue();
            }
        };
        Op EQ = new ComparisonOp("=="){
            @Override
            public Object apply(Object left, Object right) {
                return left.equals(right);
            }
        };
        Op NE = new ComparisonOp("!="){
            @Override
            public Object apply(Object left, Object right) {
                return !left.equals(right);
            }
        };

        Op BOOLEAN_AND = new BooleanOp("&&"){
            @Override
            public Object apply(Object left, Object right) {
                return ((Boolean) left) && ((Boolean) right);
            }
        };
        Op BOOLEAN_OR = new BooleanOp("||"){
            @Override
            public Object apply(Object left, Object right) {
                return ((Boolean) left) || ((Boolean) right);
            }
        };

        default boolean checkTypes(Type left, Type right){
            return left.equals(right);
        }

        default Type getResultingType(Type left, Type right){
            return left;
        }

        Object apply(Object left, Object right);
    }

    private abstract static class NumberOp implements Op{
        private final String name;

        public NumberOp(String name){
            this.name = name;
        }

        @Override
        public boolean checkTypes(Type left, Type right) {
            if(!left.equals(right)) {
                return false;
            }

            //Make sure they are both numerical types
            return Type.BOOLEAN <= left.getSort() && left.getSort() <= Type.DOUBLE;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private abstract static class ComparisonOp implements Op{
        private final String name;

        public ComparisonOp(String name){
            this.name = name;
        }

        @Override
        public boolean checkTypes(Type left, Type right) {
            return left.equals(right);
        }

        @Override
        public Type getResultingType(Type left, Type right) {
            return Type.BOOLEAN_TYPE;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private abstract static class BooleanOp implements Op{
        private final String name;

        public BooleanOp(String name){
            this.name = name;
        }

        @Override
        public boolean checkTypes(Type left, Type right) {
            return left.equals(Type.BOOLEAN_TYPE) && right.equals(Type.BOOLEAN_TYPE);
        }

        @Override
        public Type getResultingType(Type left, Type right) {
            return Type.BOOLEAN_TYPE;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
