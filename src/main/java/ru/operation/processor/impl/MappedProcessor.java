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
public class MappedProcessor<I, M, O> extends AbstractProcessor<I, O> {

    private final Processor<I, M> mapper;
    private final Processor<M, O> delegate;

    @Override
    protected Processed<O> processInternal(I input) {
        return mapper.process(input).flatMap(delegate);
    }
}
