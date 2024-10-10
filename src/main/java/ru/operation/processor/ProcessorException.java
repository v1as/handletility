package ru.operation.processor;

import ru.operation.OperationException;

public class ProcessorException extends OperationException {

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(Exception cause) {
        super(cause);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
