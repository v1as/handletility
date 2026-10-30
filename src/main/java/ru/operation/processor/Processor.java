package ru.operation.processor;

import java.util.function.Function;
import java.util.function.Predicate;
import ru.operation.handler.Handler;

/**
 * Transforms {@code I} into {@code O}. Prefer extending {@link AbstractProcessor} for check,
 * exception handling and logging.
 *
 * <p>Default chaining methods follow {@link Processed} semantics: skip/error propagate and later
 * steps are not invoked.
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
     * Keeps a successful output only if {@code predicate} holds; otherwise {@link
     * Processed#skipped()}. Skip/error from this processor propagate unchanged.
     */
    default Processor<I, O> filter(Predicate<? super O> predicate) {
        return input -> {
            Processed<O> result = process(input);
            if (result.isError() || result.isSkipped()) {
                return result;
            }
            return predicate.test(result.value()) ? result : Processed.skipped();
        };
    }

    /** Continues with a handler ({@link Processed#thenHandle}). */
    default Handler<I> thenHandle(Handler<? super O> handler) {
        return input -> process(input).thenHandle(handler::handle);
    }
}
