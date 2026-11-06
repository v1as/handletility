package ru.operation.handler;

import static lombok.AccessLevel.PACKAGE;
import static ru.operation.handler.Handled.done;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.util.function.ThrowableConsumer;

@RequiredArgsConstructor(access = PACKAGE)
final class ConsumerHandler<I> extends AbstractHandler<I> {

    @NonNull private final ThrowableConsumer<I> consumer;

    @Override
    protected Handled handleInternal(I input) throws Exception {
        consumer.accept(input);
        return done();
    }
}
