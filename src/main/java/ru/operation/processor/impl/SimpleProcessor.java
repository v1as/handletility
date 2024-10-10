package ru.operation.processor.impl;

import static ru.operation.processor.Processed.processed;
import static ru.operation.processor.Processed.skipped;

import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;

@RequiredArgsConstructor
public class SimpleProcessor<I, O> extends AbstractProcessor<I, O> {

    private final Predicate<I> check;
    private final Function<I, O> transform;

    public SimpleProcessor(Function<I, O> transform) {
        this(input -> true, transform);
    }

    @Override
    protected boolean check(I input) {
        return check.test(input);
    }

    @Override
    protected Processed<O> processInternal(I input) {
        O transformed = transform.apply(input);
        return transformed == null ? skipped() : processed(transformed);
    }
}
