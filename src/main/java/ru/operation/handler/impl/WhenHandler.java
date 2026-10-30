package ru.operation.handler.impl;

import static ru.operation.handler.Handled.skipped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Runs {@code delegate} only when {@code predicate} holds. A failed check returns {@link
 * Handled#skipped()} and does not call the delegate. Exceptions from the predicate propagate
 * unchanged, including checked ones.
 */
@RequiredArgsConstructor
public class WhenHandler<I> implements Handler<I> {

    @NonNull private final ThrowablePredicate<I> predicate;
    @NonNull private final Handler<I> delegate;

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
