package ru.operation.processor.impl;

import static ru.operation.processor.Processed.skipped;
import static ru.operation.util.Checks.notNull;

import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Runs {@code delegate} only when {@code predicate} holds. A failed check returns {@link
 * Processed#skipped()} and does not call the delegate. Exceptions from the predicate propagate
 * unchanged, including checked ones.
 */
public class WhenProcessor<I, O> implements Processor<I, O> {

    private final ThrowablePredicate<I> predicate;
    private final Processor<I, O> delegate;

    public WhenProcessor(ThrowablePredicate<I> predicate, Processor<I, O> delegate) {
        this.predicate = notNull(predicate, "predicate");
        this.delegate = notNull(delegate, "delegate");
    }

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
