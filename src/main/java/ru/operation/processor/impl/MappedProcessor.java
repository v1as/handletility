package ru.operation.processor.impl;

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

    private final Processor<T, I> mapper;
    private final Processor<I, O> delegate;

    @Override
    protected Processed<O> processInternal(T input) {
        return mapper.process(input).flatMap(delegate);
    }
}
