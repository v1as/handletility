package ru.operation.handler;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;
import static ru.operation.util.Checks.notNull;

import ru.operation.handler.impl.ConsumerHandler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.util.function.ThrowableConsumer;

/**
 * Side-effect operation over an input. Prefer extending {@link AbstractHandler} for check,
 * exception handling and logging.
 *
 * <p>{@link #and} runs the next handler on the same input when this one returns {@link
 * Handled#done()} and stops on skip or error. {@link #or} tries the next handler on the same input
 * when this one returns {@link Handled#skipped()} and stops on done or error. {@link #thenProcess}
 * runs a processor on that same input when this handler returns {@link Handled#done()}.
 */
@FunctionalInterface
public interface Handler<I> {

    Handled handle(I input);

    /**
     * When this handler returns {@link Handled#done()}, returns the result of {@code next} for the
     * same input. {@link Handled#skipped()} and {@link Handled#error(Exception)} become that outcome
     * on {@link Processed}, and {@code next} is not called.
     */
    default <O> Processor<I, O> thenProcess(Processor<? super I, O> next) {
        return input -> {
            Handled handled = handle(input);
            if (handled.isError()) {
                return error(handled.exception());
            }
            if (handled.isDone()) {
                return next.process(input);
            }
            return skipped();
        };
    }

    /**
     * Runs {@code next} on the same input when this handler returns {@link Handled#done()}. Skip and
     * error are returned unchanged and {@code next} is not called. The chain result is the outcome
     * of {@code next}.
     */
    default Handler<I> and(Handler<? super I> next) {
        return input -> {
            Handled result = handle(input);
            if (result.isError()) {
                return result;
            }
            if (result.isDone()) {
                return next.handle(input);
            }
            return result;
        };
    }

    /**
     * Tries {@code next} on the same input when this handler returns {@link Handled#skipped()}.
     * {@code DONE} and {@code ERROR} are returned unchanged and {@code next} is not called. A chain
     * stops at the first done or error and stays skipped only when every step skips.
     */
    default Handler<I> or(Handler<? super I> next) {
        return input -> {
            Handled result = handle(input);
            if (result.isError()) {
                return result;
            }
            if (result.isDone()) {
                return result;
            }
            return next.handle(input);
        };
    }

    /** Identifies this handler by a constant routing key. */
    default <K> IdentifiedHandler<K, I> identifiedBy(K identifier) {
        return new ConstantIdentifiedHandler<>(identifier, this);
    }

    /** Bare handler from a {@link ThrowableConsumer}. A thrown exception is {@link Handled#error}. */
    static <A> Handler<A> of(ThrowableConsumer<A> consumer) {
        return new ConsumerHandler<>(notNull(consumer, "consumer"));
    }
}
