package ru.operation.handler.impl;

import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.util.function.ThrowableConsumer;

@RequiredArgsConstructor
public class SimpleHandler<I> extends AbstractHandler<I> {

    private final Predicate<I> check;
    private final ThrowableConsumer<I> consumer;

    public SimpleHandler(ThrowableConsumer<I> consumer) {
        this(SimpleHandler::alwaysTrue, consumer);
    }

    @Override
    public boolean check(I input) {
        return check.test(input);
    }

    @Override
    protected Handled handleInternal(I input) throws Exception {
        consumer.accept(input);
        return Handled.handled();
    }

    private static <T> boolean alwaysTrue(T item) {
        return true;
    }
}
