package ru.operation.handler;

import ru.operation.OperationException;

public class HandlerException extends OperationException {

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
