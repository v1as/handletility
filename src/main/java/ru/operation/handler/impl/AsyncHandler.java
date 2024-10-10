package ru.operation.handler.impl;

import java.util.concurrent.Executor;
import ru.operation.handler.AbstractAsyncHandler;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;

public class AsyncHandler<T> extends AbstractAsyncHandler<T> {

    private final AbstractHandler<T> handler;

    public AsyncHandler(Executor executor, AbstractHandler<T> handler) {
        super(executor);
        this.handler = handler;
    }

    @Override
    public boolean check(T input) {
        return handler.check(input);
    }

    @Override
    protected Handled handleAsync(T input) {
        return handler.handle(input);
    }
}
