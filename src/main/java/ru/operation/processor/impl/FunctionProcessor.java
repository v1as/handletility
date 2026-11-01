package ru.operation.processor.impl;

import static ru.operation.processor.Processed.done;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.util.function.ThrowableFunction;

@RequiredArgsConstructor
public class FunctionProcessor<I, O> extends AbstractProcessor<I, O> {

    @NonNull private final ThrowableFunction<I, O> transform;

    @Override
    protected Processed<O> processInternal(I input) throws Exception {
        return done(transform.apply(input));
    }
}
