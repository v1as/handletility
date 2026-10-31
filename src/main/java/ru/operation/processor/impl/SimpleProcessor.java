package ru.operation.processor.impl;

import static ru.operation.processor.Processed.of;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.util.function.ThrowableFunction;

@RequiredArgsConstructor
public class SimpleProcessor<I, O> extends AbstractProcessor<I, O> {

    @NonNull private final ThrowableFunction<I, O> transform;

    @Override
    protected Processed<O> processInternal(I input) throws Exception {
        return of(transform.apply(input));
    }
}
