package ru.operation.handler.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processor;

/**
 * Runs {@code mapper} then {@code delegate}. Skip/error from the mapper propagate (same as {@link
 * ru.operation.processor.Processed#thenHandle}); the delegate is not called.
 */
@RequiredArgsConstructor
public class MappedHandler<I, M> extends AbstractHandler<I> {

    @NonNull private final Processor<I, M> mapper;
    @NonNull private final Handler<M> delegate;

    @Override
    protected Handled handleInternal(I input) {
        return mapper.process(input).thenHandle(delegate);
    }
}
