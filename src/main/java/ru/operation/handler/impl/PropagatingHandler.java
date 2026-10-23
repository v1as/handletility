package ru.operation.handler.impl;

import static ru.operation.util.Checks.notNull;

import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.util.function.ThrowableConsumer;

/**
 * Runs a consumer and lets exceptions escape, so an outer catching wrapper can map them. Unlike
 * {@link SimpleHandler}, this does not turn failures into {@link Handled#error(Exception)} itself.
 */
public class PropagatingHandler<I> implements Handler<I> {

    private final ThrowableConsumer<I> consumer;

    public PropagatingHandler(ThrowableConsumer<I> consumer) {
        this.consumer = notNull(consumer, "consumer");
    }

    @Override
    public Handled handle(I input) {
        try {
            consumer.accept(input);
            return Handled.ok();
        } catch (Exception e) {
            throw unchecked(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable exception) throws E {
        throw (E) exception;
    }

    @Override
    public String toString() {
        return "Propagating(" + consumer + ")";
    }
}
