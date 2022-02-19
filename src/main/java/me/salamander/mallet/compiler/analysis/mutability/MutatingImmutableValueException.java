package me.salamander.mallet.compiler.analysis.mutability;

public class MutatingImmutableValueException extends RuntimeException {
    public MutatingImmutableValueException() {
    }

    public MutatingImmutableValueException(String message) {
        super(message);
    }

    public MutatingImmutableValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public MutatingImmutableValueException(Throwable cause) {
        super(cause);
    }

    public MutatingImmutableValueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
