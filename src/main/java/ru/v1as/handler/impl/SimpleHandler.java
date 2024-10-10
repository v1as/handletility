package ru.v1as.handler.impl;

import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;

@RequiredArgsConstructor
public class SimpleHandler<I> extends AbstractHandler<I> {

    private final Predicate<I> check;
    private final Consumer<I> consumer;

    public SimpleHandler(Consumer<I> consumer) {
        this(SimpleHandler::alwaysTrue, consumer);
    }

    @Override
    protected boolean check(I input) {
        return check.test(input);
    }

    @Override
    protected Handled handleInternal(I input) {
        consumer.accept(input);
        return Handled.handled();
    }

    private static <T> boolean alwaysTrue(T item) {
        return true;
    }
}
