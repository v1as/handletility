package ru.operation.handler.impl;

import static ru.operation.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.Builder;
import lombok.NonNull;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/**
 * AOP-style around: {@code before(input)} → {@code handler(input)} → {@code after(result)}. The
 * returned {@link Handled} is always from the main handler; after only observes it.
 */
@Builder
public class AroundHandler<I> implements Handler<I> {

    private final Handler<I> beforeHandler;
    private final Handler<I> handler;
    private final Handler<Handled> afterHandler;

    public AroundHandler(
            Handler<I> beforeHandler, @NonNull Handler<I> handler, Handler<Handled> afterHandler) {
        this.beforeHandler = beforeHandler != null ? beforeHandler : alwaysSkippedHandler();
        this.handler = handler;
        this.afterHandler = afterHandler != null ? afterHandler : alwaysSkippedHandler();
    }

    @Override
    public Handled handle(I input) {
        beforeHandler.handle(input);
        Handled result = handler.handle(input);
        afterHandler.handle(result);
        return result;
    }

    @Override
    public String toString() {
        return "Around(before="
                + beforeHandler
                + ", handler="
                + handler
                + ", after="
                + afterHandler
                + ")";
    }
}
