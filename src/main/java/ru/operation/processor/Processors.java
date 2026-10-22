package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import ru.operation.processor.impl.SimpleProcessor;
import ru.operation.util.function.ThrowableFunction;

/** Facade for composing processors with naming, logging and MDC wrappers. */
public final class Processors {

    private Processors() {}

    /**
     * Bare processor from a {@link ThrowableFunction} (null result → {@link
     * Processed#skipped()}).
     */
    public static <I, O> Processor<I, O> of(ThrowableFunction<I, O> transform) {
        return new SimpleProcessor<>(notNull(transform, "transform"));
    }

    public static <I, O> ProcessorCompose<I, O> compose(Processor<I, O> core) {
        return ProcessorCompose.from(notNull(core, "core"));
    }

    public static <I, O> ProcessorCompose<I, O> compose(ThrowableFunction<I, O> transform) {
        return compose(of(transform));
    }
}
