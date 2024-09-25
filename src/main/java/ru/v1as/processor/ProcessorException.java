package ru.v1as.processor;

public class ProcessorException extends RuntimeException {

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(Exception cause) {
        super(cause);
    }
}
