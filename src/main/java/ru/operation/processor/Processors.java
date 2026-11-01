package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import java.util.function.Predicate;
import ru.operation.processor.impl.FilterProcessor;
import ru.operation.processor.impl.FunctionProcessor;
import ru.operation.processor.impl.ThrowingProcessor;
import ru.operation.util.function.ThrowableFunction;

/** Facade for composing processors with naming, logging and MDC wrappers. */
public final class Processors {

    private Processors() {}

    /** Bare processor from a {@link ThrowableFunction} ({@code null} result → {@link Processed#done}). */
    public static <I, O> Processor<I, O> of(ThrowableFunction<I, O> transform) {
        return new FunctionProcessor<>(notNull(transform, "transform"));
    }

    /**
     * Starts a chain from the input. The input is {@link Processed#done(Object) done} with that same
     * value when {@code predicate} holds, and {@link Processed#skipped()} otherwise. An exception
     * from the predicate is {@link Processed#error(Exception)}. Later {@link Processor#filter}
     * checks a successful output of this chain.
     */
    public static <I> Processor<I, I> filter(Predicate<? super I> predicate) {
        return new FilterProcessor<>(notNull(predicate, "predicate"));
    }

    public static <I, O> ProcessorCompose<I, O> compose(Processor<I, O> core) {
        return ProcessorCompose.from(notNull(core, "core"));
    }

    /**
     * Compose entry for a function ({@code null} result → {@link Processed#done}). Exceptions
     * propagate to {@link ProcessorCompose#onException}; {@link #of} keeps them inside {@link
     * FunctionProcessor}.
     */
    public static <I, O> ProcessorCompose<I, O> compose(ThrowableFunction<I, O> transform) {
        return compose(new ThrowingProcessor<>(notNull(transform, "transform")));
    }
}
