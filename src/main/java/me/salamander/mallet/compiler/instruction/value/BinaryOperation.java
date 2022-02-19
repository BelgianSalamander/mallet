package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
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

    public interface Op{
        Op ADD = new NumberOp("+");
        Op SUB = new NumberOp("-");
        Op MUL = new NumberOp("*");
        Op DIV = new NumberOp("/");
        Op REM = new NumberOp("%");
        Op AND = new NumberOp("&");
        Op OR = new NumberOp("|");
        Op XOR = new NumberOp("^");
        Op SHL = new NumberOp("<<");
        Op SHR = new NumberOp(">>");
        Op USHR = new NumberOp(">>>");
        Op CMP = new NumberOp("cmp");

        Op LT = new ComparisonOp("<");
        Op LE = new ComparisonOp("<=");
        Op GT = new ComparisonOp(">");
        Op GE = new ComparisonOp(">=");
        Op EQ = new ComparisonOp("==");
        Op NE = new ComparisonOp("!=");

        default boolean checkTypes(Type left, Type right){
            return left.equals(right);
        }

        default Type getResultingType(Type left, Type right){
            return left;
        }
    }

    private static class NumberOp implements Op{
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

    private static class ComparisonOp implements Op{
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
}
