package ru.operation.processor;

import java.util.function.Function;
import java.util.function.Predicate;
import ru.operation.handler.Handler;

/**
 * Transforms {@code I} into {@code O}. Prefer extending {@link AbstractProcessor} for check,
 * exception handling and logging.
 *
 * <p>{@link #thenApply}, {@link #thenCompose}, {@link #filter} and {@link #thenHandle} follow {@link
 * Processed}: skip and error propagate, and later steps are not invoked. {@link #or} is the
 * opposite on skip: it tries the next processor on the same input and still stops on done or
 * error.
 */
@FunctionalInterface
public interface Processor<I, O> {

    Processed<O> process(I input);

    /** Maps a successful output ({@link Processed#map}). */
    default <R> Processor<I, R> thenApply(Function<? super O, ? extends R> transform) {
        return input -> process(input).map(transform::apply);
    }

    /** Chains another processor ({@link Processed#flatMap}). */
    default <R> Processor<I, R> thenCompose(Processor<? super O, R> next) {
        return input -> process(input).flatMap(next::process);
    }

    /**
     * Tries {@code next} on the same input when this processor returns {@link Processed#skipped()}.
     * {@code DONE} and {@code ERROR} are returned unchanged and {@code next} is not called. A chain
     * stops at the first done or error and stays skipped only when every step skips.
     */
    default Processor<I, O> or(Processor<? super I, O> next) {
        return input -> {
            Processed<O> result = process(input);
            return result.isSkipped() ? next.process(input) : result;
        };
    }

    /**
     * Keeps a successful output only if {@code predicate} holds; otherwise {@link
     * Processed#skipped()}. Skip/error from this processor propagate unchanged. Delegates to
     * {@link Processed#filter}: an exception from {@code predicate} becomes {@link
     * Processed#error(Exception)}.
     */
    default Processor<I, O> filter(Predicate<? super O> predicate) {
        return input -> process(input).filter(predicate);
    }

    /** Continues with a handler ({@link Processed#thenHandle}). */
    default Handler<I> thenHandle(Handler<? super O> handler) {
        return input -> process(input).thenHandle(handler::handle);
    }
}
