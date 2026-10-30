package ru.operation.handler.impl;

import java.util.function.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.util.function.ThrowableConsumer;

@RequiredArgsConstructor
public class SimpleHandler<I> extends AbstractHandler<I> {

    @NonNull private final Predicate<I> predicate;
    @NonNull private final ThrowableConsumer<I> consumer;

    public SimpleHandler(ThrowableConsumer<I> consumer) {
        this(input -> true, consumer);
    }

    @Override
    public boolean check(I input) {
        return predicate.test(input);
    }

    @Override
    protected Handled handleInternal(I input) throws Exception {
        consumer.accept(input);
        return Handled.done();
    }
}
