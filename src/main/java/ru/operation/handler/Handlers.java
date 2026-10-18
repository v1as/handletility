package ru.operation.handler;

import static ru.operation.util.Checks.notNull;

import java.util.function.Consumer;
import ru.operation.handler.impl.SimpleHandler;

/** Facade for composing handlers with naming, logging and MDC wrappers. */
public final class Handlers {

    private Handlers() {}

    public static <I> HandlerCompose<I> compose(Handler<I> core) {
        return HandlerCompose.from(notNull(core, "core"));
    }

    public static <I> HandlerCompose<I> compose(Consumer<I> consumer) {
        return compose(new SimpleHandler<>(notNull(consumer, "consumer")::accept));
    }
}
