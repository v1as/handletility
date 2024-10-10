package ru.v1as.handler;

import ru.v1as.OperationException;

public class HandlerException extends OperationException {

    public HandlerException(Throwable cause) {
        super(cause);
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
