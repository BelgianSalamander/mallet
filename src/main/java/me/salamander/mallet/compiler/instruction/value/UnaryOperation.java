package me.salamander.mallet.compiler.instruction.value;

import me.salamander.mallet.compiler.analysis.mutability.Mutability;
import me.salamander.mallet.compiler.analysis.mutability.MutabilityValue;
import me.salamander.mallet.util.ASMUtil;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class UnaryOperation implements Value{
    private Value arg;
    private Op op;

    public UnaryOperation(Value value, Op op){
        this.arg = value;
        this.op = op;
    }

    public Value getArg() {
        return arg;
    }

    public Op getOp() {
        return op;
    }

    public void setArg(Value arg) {
        this.arg = arg;
        checkType();
    }

    public void setOp(Op op) {
        this.op = op;
        checkType();
    }

    private void checkType(){
        if(!op.checkType(arg.getType())){
            throw new IllegalStateException("Invalid type for operation");
        }
    }

    @Override
    public Type getType() {
        return op.getResultingType(arg.getType());
    }

    @Override
    public boolean isInvalidatedByChangeIn(Value value) {
        return arg.isInvalidatedByChangeIn(value);
    }

    @Override
    public List<Variable> usedVariables() {
        return arg.usedVariables();
    }

    @Override
    public boolean allowInline() {
        return arg.allowInline();
    }

    @Override
    public boolean allowDuplicateInline() {
        return arg.allowDuplicateInline();
    }

    @Override
    public Value copyValue(Function<Value, Value> innerValueCopier) {
        return new UnaryOperation(arg.copyValue(innerValueCopier), op);
    }

    @Override
    public Mutability getMutability(MutabilityValue varMutability) {
        return Mutability.IMMUTABLE;
    }

    public interface Op{
        Op NEG = new NumberOp("-");

        Op TO_INT = new NumberConvertOp(Type.INT_TYPE, "(int) ");
        Op TO_LONG = new NumberConvertOp(Type.LONG_TYPE, "(long) ");
        Op TO_FLOAT = new NumberConvertOp(Type.FLOAT_TYPE, "(float) ");
        Op TO_DOUBLE = new NumberConvertOp(Type.DOUBLE_TYPE, "(double) ");

        Op ARRAY_LENGTH = new Op() {
            @Override
            public boolean checkType(Type type) {
                return type.getSort() == Type.ARRAY;
            }

            @Override
            public Type getResultingType(Type type) {
                return Type.INT_TYPE;
            }
        };

        Op ISNULL = NullOp.ISNULL;
        Op ISNOTNULL = NullOp.ISNOTNULL;

        static Op makeCheckCast(Type type){
            return new Op() {
                @Override
                public boolean checkType(Type type) {
                    return true;
                }

                @Override
                public Type getResultingType(Type t) {
                    return type;
                }
            };
        }

        static Op makeInstanceOf(Type type){
            return new Op() {
                @Override
                public boolean checkType(Type type) {
                    return true;
                }

                @Override
                public Type getResultingType(Type t) {
                    return Type.BOOLEAN_TYPE;
                }
            };
        }

        boolean checkType(Type type);
        Type getResultingType(Type type);
    }

    private static class NumberOp implements Op{
        private final String name;

        public NumberOp(String name) {
            this.name = name;
        }

        @Override
        public boolean checkType(Type type) {
            return ASMUtil.isNumber(type);
        }

        @Override
        public Type getResultingType(Type type) {
            return type;
        }
    }

    private static class NumberConvertOp implements Op{
        private final Type resultType;
        private final String name;

        public NumberConvertOp(Type resultType, String name) {
            this.resultType = resultType;
            this.name = name;
        }

        @Override
        public boolean checkType(Type type) {
            return ASMUtil.isNumber(type);
        }

        @Override
        public Type getResultingType(Type type) {
            return resultType;
        }
    }

    private enum NullOp implements Op{
        ISNULL(false),
        ISNOTNULL(true);

        private final boolean inverted;

        NullOp(boolean inverted) {
            this.inverted = inverted;
        }

        @Override
        public boolean checkType(Type type) {
            return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
        }

        @Override
        public Type getResultingType(Type type) {
            return Type.BOOLEAN_TYPE;
        }
    }
}
