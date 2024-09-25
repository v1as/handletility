package ru.v1as.handler;

public class HandlerException extends RuntimeException {

    public HandlerException(Throwable cause) {
        super(cause);
    }

    public HandlerException(String message) {
        super(message);
    }
}
