package ru.operation.handler;

import static ru.operation.util.Checks.notNull;

import ru.operation.handler.impl.ConsumerHandler;
import ru.operation.handler.impl.ThrowingHandler;
import ru.operation.util.function.ThrowableConsumer;

/** Facade for composing handlers with naming, logging and MDC wrappers. */
public final class Handlers {

    private Handlers() {}

    /** Bare handler from a {@link ThrowableConsumer}. */
    public static <I> Handler<I> of(ThrowableConsumer<I> consumer) {
        return new ConsumerHandler<>(notNull(consumer, "consumer"));
    }

    public static <I> HandlerCompose<I> compose(Handler<I> core) {
        return HandlerCompose.from(notNull(core, "core"));
    }

    /**
     * Compose entry for a consumer. Exceptions propagate to {@link HandlerCompose#onException};
     * {@link #of} keeps them inside {@link ConsumerHandler}.
     */
    public static <I> HandlerCompose<I> compose(ThrowableConsumer<I> consumer) {
        return compose(new ThrowingHandler<>(notNull(consumer, "consumer")));
    }
}
