package ru.operation.handler.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.Failed;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
@RequiredArgsConstructor
public class CatchingHandler<I> implements Handler<I> {

    @NonNull private final Handler<I> delegate;
    @NonNull private final Handler<Failed<I>> onException;

    public CatchingHandler(Handler<I> delegate) {
        this(delegate, new LoggingExceptionHandler<>());
    }

    @Override
    public Handled handle(I input) {
        try {
            return delegate.handle(input);
        } catch (Exception e) {
            return onException.handle(new Failed<>(input, e));
        }
    }

    @Override
    public String toString() {
        return "Catching(" + delegate + ")";
    }
}
