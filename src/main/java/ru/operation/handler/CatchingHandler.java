package ru.operation.handler;

import static lombok.AccessLevel.PACKAGE;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.Failure;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
@RequiredArgsConstructor(access = PACKAGE)
final class CatchingHandler<I> implements Handler<I> {

    @NonNull private final Handler<I> delegate;
    @NonNull private final Handler<Failure<I>> onException;

    public CatchingHandler(Handler<I> delegate) {
        this(delegate, new LoggingExceptionHandler<>());
    }

    @Override
    public Handled handle(I input) {
        try {
            return delegate.handle(input);
        } catch (Exception e) {
            return onException.handle(new Failure<>(input, e));
        }
    }

    @Override
    public String toString() {
        return "Catching(" + delegate + ")";
    }
}
