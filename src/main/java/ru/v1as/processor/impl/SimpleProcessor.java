package ru.v1as.processor.impl;

import static ru.v1as.processor.Processed.processed;
import static ru.v1as.processor.Processed.skipped;

import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import ru.v1as.processor.AbstractProcessor;
import ru.v1as.processor.Processed;

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
