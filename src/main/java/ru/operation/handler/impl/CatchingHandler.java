package ru.operation.handler.impl;

import static ru.operation.util.Checks.notNull;

import ru.operation.Failed;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
public class CatchingHandler<I> implements Handler<I> {

    private final Handler<I> delegate;
    private final Handler<Failed<I>> onException;

    public CatchingHandler(Handler<I> delegate) {
        this(notNull(delegate, "delegate"), new LoggingExceptionHandler<>());
    }

    public CatchingHandler(Handler<I> delegate, Handler<Failed<I>> onException) {
        this.delegate = notNull(delegate, "delegate");
        this.onException = notNull(onException, "onException");
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
