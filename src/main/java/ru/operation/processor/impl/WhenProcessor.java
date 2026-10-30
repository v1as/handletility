package ru.operation.processor.impl;

import static ru.operation.processor.Processed.skipped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Runs {@code delegate} only when {@code predicate} holds. A failed check returns {@link
 * Processed#skipped()} and does not call the delegate. Exceptions from the predicate propagate
 * unchanged, including checked ones.
 */
@RequiredArgsConstructor
public class WhenProcessor<I, O> implements Processor<I, O> {

    @NonNull private final ThrowablePredicate<I> predicate;
    @NonNull private final Processor<I, O> delegate;

    @Override
    public Processed<O> process(I input) {
        if (!test(input)) {
            return skipped();
        }
        return delegate.process(input);
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
