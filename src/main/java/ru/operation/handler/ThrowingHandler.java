package ru.operation.handler;

import static ru.operation.handler.Handled.done;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.util.function.ThrowableConsumer;

/**
 * Runs a consumer and lets exceptions escape, so an outer catching wrapper can map them. Unlike
 * {@link ConsumerHandler}, this does not turn failures into {@link Handled#error(Exception)} itself.
 */
@RequiredArgsConstructor
public final class ThrowingHandler<I> implements Handler<I> {

    @NonNull private final ThrowableConsumer<I> consumer;

    @Override
    public Handled handle(I input) {
        try {
            consumer.accept(input);
            return done();
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
        return "Throwing(" + consumer + ")";
    }
}
