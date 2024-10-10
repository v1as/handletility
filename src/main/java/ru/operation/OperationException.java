package ru.operation;

public class OperationException extends RuntimeException {

    public OperationException(Throwable cause) {
        super(cause);
    }

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
