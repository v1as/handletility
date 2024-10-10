package ru.operation.handler.impl;

import static ru.operation.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.Builder;
import lombok.NonNull;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

@Builder
public class CompositeHandler<O> implements Handler<O> {

    private final Handler<O> beforeHandler;
    private final Handler<O> handler;
    private final Handler<O> afterHandler;

    public CompositeHandler(
            Handler<O> beforeHandler, @NonNull Handler<O> handler, Handler<O> afterHandler) {
        this.beforeHandler = beforeHandler != null ? beforeHandler : alwaysSkippedHandler();
        this.handler = handler;
        this.afterHandler = afterHandler != null ? afterHandler : alwaysSkippedHandler();
    }

    @Override
    public Handled handle(O input) {
        beforeHandler.handle(input);
        try {
            return handler.handle(input);
        } finally {
            afterHandler.handle(input);
        }
    }
}
