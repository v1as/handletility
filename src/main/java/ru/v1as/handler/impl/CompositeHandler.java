package ru.v1as.handler.impl;

import static ru.v1as.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.RequiredArgsConstructor;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;

@RequiredArgsConstructor
public class CompositeHandler<O> implements Handler<O> {

    private final Handler<O> beforeHandler;
    private final Handler<O> handler;
    private final Handler<O> afterHandler;

    @Override
    public Handled handle(O input) {
        beforeHandler.handle(input);
        try {
            return handler.handle(input);
        } finally {
            afterHandler.handle(input);
        }
    }

    public static <T> Handler<T> withBefore(Handler<T> handler, Handler<T> before) {
        return new CompositeHandler<>(before, handler, alwaysSkippedHandler());
    }

    public static <T> Handler<T> withAfter(Handler<T> handler, Handler<T> after) {
        return new CompositeHandler<>(alwaysSkippedHandler(), handler, after);
    }
}
