package ru.operation.handler.impl;

import static ru.operation.handler.Handled.skipped;

import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class AlwaysSkippedHandler implements Handler<Object> {

    private static final AlwaysSkippedHandler INSTANCE = new AlwaysSkippedHandler();

    @Override
    public Handled handle(Object input) {
        return skipped();
    }

    @SuppressWarnings("unchecked")
    public static <T> Handler<T> alwaysSkippedHandler() {
        return (Handler<T>) INSTANCE;
    }
}
