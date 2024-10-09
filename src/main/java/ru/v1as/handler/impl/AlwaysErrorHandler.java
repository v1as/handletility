package ru.v1as.handler.impl;

import static ru.v1as.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.HandlerException;

@RequiredArgsConstructor
public class AlwaysErrorHandler implements Handler<Object> {

    private Handled error;

    public AlwaysErrorHandler(Exception exception) {
        this.error = error(exception);
    }

    public AlwaysErrorHandler(String reason) {
        this(new HandlerException(reason));
    }

    @Override
    public Handled handle(Object input) {
        return error;
    }
}
