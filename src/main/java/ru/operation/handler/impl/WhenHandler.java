package ru.operation.handler.impl;

import static ru.operation.handler.Handled.skipped;
import static ru.operation.util.Checks.notNull;

import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Runs {@code delegate} only when {@code predicate} holds. A failed check returns {@link
 * Handled#skipped()} and does not call the delegate. Exceptions from the predicate propagate
 * unchanged, including checked ones.
 */
public class WhenHandler<I> implements Handler<I> {

    private final ThrowablePredicate<I> predicate;
    private final Handler<I> delegate;

    public WhenHandler(ThrowablePredicate<I> predicate, Handler<I> delegate) {
        this.predicate = notNull(predicate, "predicate");
        this.delegate = notNull(delegate, "delegate");
    }

    @Override
    public Handled handle(I input) {
        if (!test(input)) {
            return skipped();
        }
        return delegate.handle(input);
    }

    private boolean test(I input) {
        try {
            return predicate.test(input);
        } catch (Exception e) {
            throw unchecked(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable exception) throws E {
        throw (E) exception;
    }

    @Override
    public String toString() {
        return "When(" + delegate + ")";
    }
}
