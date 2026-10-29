package ru.operation.processor;

import java.util.List;
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

    public ProcessorException(List<? extends Throwable> causes) {
        super(causes);
    }
}
