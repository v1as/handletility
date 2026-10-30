package ru.operation.processor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.Failed;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
@RequiredArgsConstructor
public class CatchingProcessor<I, O> implements Processor<I, O> {

    @NonNull private final Processor<I, O> delegate;
    @NonNull private final Processor<Failed<I>, O> onException;

    public CatchingProcessor(Processor<I, O> delegate) {
        this(delegate, new LoggingExceptionProcessor<>());
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
