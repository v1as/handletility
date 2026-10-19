package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import java.util.function.Function;
import ru.operation.processor.impl.SimpleProcessor;

/** Facade for composing processors with naming, logging and MDC wrappers. */
public final class Processors {

    private Processors() {}

    /** Bare processor from a {@link Function} (null result → {@link Processed#skipped()}). */
    public static <I, O> Processor<I, O> of(Function<I, O> transform) {
        return new SimpleProcessor<>(notNull(transform, "transform"));
    }

    public static <I, O> ProcessorCompose<I, O> compose(Processor<I, O> core) {
        return ProcessorCompose.from(notNull(core, "core"));
    }

    public static <I, O> ProcessorCompose<I, O> compose(Function<I, O> transform) {
        return compose(of(transform));
    }
}
