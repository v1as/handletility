package ru.operation.processor.impl;

import static ru.operation.processor.Processed.of;

import java.util.function.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.util.function.ThrowableFunction;

@RequiredArgsConstructor
public class SimpleProcessor<I, O> extends AbstractProcessor<I, O> {

    @NonNull private final Predicate<I> check;
    @NonNull private final ThrowableFunction<I, O> transform;

    public SimpleProcessor(ThrowableFunction<I, O> transform) {
        this(input -> true, transform);
    }

    @Override
    protected boolean check(I input) {
        return check.test(input);
    }

    @Override
    protected Processed<O> processInternal(I input) throws Exception {
        return of(transform.apply(input));
    }
}
