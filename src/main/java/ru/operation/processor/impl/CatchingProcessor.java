package ru.operation.processor.impl;

import static ru.operation.util.Checks.notNull;

import ru.operation.Failed;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
public class CatchingProcessor<I, O> implements Processor<I, O> {

    private final Processor<I, O> delegate;
    private final Processor<Failed<I>, O> onException;

    public CatchingProcessor(Processor<I, O> delegate) {
        this(notNull(delegate, "delegate"), new LoggingExceptionProcessor<>());
    }

    public CatchingProcessor(Processor<I, O> delegate, Processor<Failed<I>, O> onException) {
        this.delegate = notNull(delegate, "delegate");
        this.onException = notNull(onException, "onException");
    }

    @Override
    public Processed<O> process(I input) {
        try {
            return delegate.process(input);
        } catch (Exception e) {
            return onException.process(new Failed<>(input, e));
        }
    }

    @Override
    public String toString() {
        return "Catching(" + delegate + ")";
    }
}
