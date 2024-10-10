package ru.v1as.processor;

import ru.v1as.OperationException;

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
