package ru.v1as.handler.impl;

import static ru.v1as.handler.Handled.skipped;

import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;

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
