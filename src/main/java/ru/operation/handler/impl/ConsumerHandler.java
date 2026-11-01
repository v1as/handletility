package ru.operation.handler.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.util.function.ThrowableConsumer;

@RequiredArgsConstructor
public class ConsumerHandler<I> extends AbstractHandler<I> {

    @NonNull private final ThrowableConsumer<I> consumer;

    @Override
    protected Handled handleInternal(I input) throws Exception {
        consumer.accept(input);
        return Handled.done();
    }
}
