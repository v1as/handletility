package ru.operation.handler.impl;

import static ru.operation.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerException;

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
