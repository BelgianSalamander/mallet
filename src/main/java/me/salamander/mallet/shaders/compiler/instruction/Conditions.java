package me.salamander.mallet.shaders.compiler.instruction;

import me.salamander.mallet.shaders.compiler.instruction.value.BinaryOperation;
import me.salamander.mallet.shaders.compiler.instruction.value.LiteralValue;
import me.salamander.mallet.shaders.compiler.instruction.value.UnaryOperation;
import me.salamander.mallet.shaders.compiler.instruction.value.Value;
import org.objectweb.asm.Type;

public class Conditions {
    public static Value invert(Value condition) {
        if (condition.getType() != Type.BOOLEAN_TYPE) {
            throw new IllegalArgumentException("Condition must be boolean");
        }

        if (condition instanceof LiteralValue literal) {
            return new LiteralValue(Type.BOOLEAN_TYPE, !((boolean) literal.getValue()));
        }

        if (condition instanceof UnaryOperation unaryOperation) {
            if (unaryOperation.getOp() == UnaryOperation.Op.NOT) {
                return unaryOperation.getArg();
            }
        }

        if (condition instanceof BinaryOperation binaryOperation) {
            if (binaryOperation.getOp() == BinaryOperation.Op.EQ) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.NE);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.NE) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.EQ);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.GT) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.LE);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.GE) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.LT);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.LT) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.GE);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.LE) {
                return new BinaryOperation(binaryOperation.getLeft(), binaryOperation.getRight(), BinaryOperation.Op.GT);
            } else if (binaryOperation.getOp() == BinaryOperation.Op.BOOLEAN_AND) {
                return new BinaryOperation(
                        invert(binaryOperation.getLeft()),
                        invert(binaryOperation.getRight()),
                        BinaryOperation.Op.BOOLEAN_OR
                );
            } else if (binaryOperation.getOp() == BinaryOperation.Op.BOOLEAN_OR) {
                return new BinaryOperation(
                        invert(binaryOperation.getLeft()),
                        invert(binaryOperation.getRight()),
                        BinaryOperation.Op.BOOLEAN_AND
                );
            }
        }

        return new UnaryOperation(condition, UnaryOperation.Op.NOT);
    }

    public static Value and(Value left, Value right) {
        if (left.getType() != Type.BOOLEAN_TYPE || right.getType() != Type.BOOLEAN_TYPE) {
            throw new IllegalArgumentException("Conditions must be boolean");
        }

        boolean leftLiteral = left instanceof LiteralValue;
        boolean rightLiteral = right instanceof LiteralValue;

        if (leftLiteral && rightLiteral) {
            return new LiteralValue(Type.BOOLEAN_TYPE, ((boolean) ((LiteralValue) left).getValue()) && ((boolean) ((LiteralValue) right).getValue()));
        }

        if (leftLiteral) {
            if ((boolean) ((LiteralValue) left).getValue()) {
                return right;
            } else {
                return left;
            }
        }

        if (rightLiteral) {
            if ((boolean) ((LiteralValue) right).getValue()) {
                return left;
            } else {
                return right;
            }
        }

        return new BinaryOperation(left, right, BinaryOperation.Op.BOOLEAN_AND);
    }

    public static Value or(Value left, Value right) {
        if (left.getType() != Type.BOOLEAN_TYPE || right.getType() != Type.BOOLEAN_TYPE) {
            throw new IllegalArgumentException("Conditions must be boolean");
        }

        boolean leftLiteral = left instanceof LiteralValue;
        boolean rightLiteral = right instanceof LiteralValue;

        if (leftLiteral && rightLiteral) {
            return new LiteralValue(Type.BOOLEAN_TYPE, ((boolean) ((LiteralValue) left).getValue()) || ((boolean) ((LiteralValue) right).getValue()));
        }

        if (leftLiteral) {
            if ((boolean) ((LiteralValue) left).getValue()) {
                return left;
            } else {
                return right;
            }
        }

        if (rightLiteral) {
            if ((boolean) ((LiteralValue) right).getValue()) {
                return right;
            } else {
                return left;
            }
        }

        return new BinaryOperation(left, right, BinaryOperation.Op.BOOLEAN_OR);
    }
}
