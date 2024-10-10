package ru.operation.handler;

import static ru.operation.handler.Handled.handled;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractAsyncHandler<T> extends AbstractHandler<T> {

    private final Executor executor;

    @Override
    protected Handled handleInternal(T input) {
        executor.execute(this.asyncHandleRunnable(input));
        return handled();
    }

    private Runnable asyncHandleRunnable(T input) {
        return () -> {
            try {
                Handled handled = handleAsync(input);
                if (handled.isError()) {
                    this.onException(input, handled.exception());
                }
            } catch (Exception e) {
                this.onException(input, e);
            }
        };
    }

    protected abstract Handled handleAsync(T input) throws Exception;
}
