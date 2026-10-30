package ru.operation.processor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Runs {@code mapper} then {@code delegate}. Skip/error from the mapper propagate (same as {@link
 * Processed#flatMap}); the delegate is not called.
 */
@RequiredArgsConstructor
public class MappedProcessor<T, I, O> extends AbstractProcessor<T, O> {

    @NonNull private final Processor<T, I> mapper;
    @NonNull private final Processor<I, O> delegate;

    @Override
    protected Processed<O> processInternal(T input) {
        return mapper.process(input).flatMap(delegate);
    }
}
