package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import java.util.function.Function;
import java.util.function.Predicate;
import ru.operation.Failure;
import ru.operation.handler.Handler;
import ru.operation.processor.impl.FilterProcessor;
import ru.operation.processor.impl.FunctionProcessor;
import ru.operation.util.function.ThrowableBiFunction;
import ru.operation.util.function.ThrowableFunction;

/**
 * Transforms {@code I} into {@code O}. Prefer extending {@link AbstractProcessor} for check,
 * exception handling and logging.
 *
 * <p>{@link #thenApply}, {@link #thenCompose}, {@link #filter} and {@link #thenHandle} follow {@link
 * Processed}: skip and error propagate, and later steps are not invoked. {@link #and} runs the next
 * processor on the same input only after done and folds both payloads. {@link #or} tries the next
 * processor on the same input after skip and still stops on done or error. {@link #recover} turns an
 * error into {@link Processed#done(Object)} from the original input and the exception.
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
            if (result.isError()) {
                return result;
            }
            if (result.isDone()) {
                return result;
            }
            return next.process(input);
        };
    }

    /**
     * Runs {@code next} on the same input when this processor is {@link Processed#done(Object)}. Skip
     * and error stop the chain and {@code next} is not called. When {@code next} is done, {@code
     * combiner} receives both payloads, including {@code null}, and its result is {@link
     * Processed#done(Object)}. A skip or error from {@code next} is the chain result. An exception
     * from {@code combiner} is {@link Processed#error(Exception)}. {@link Error} is not caught.
     */
    default <Q, R> Processor<I, R> and(
            Processor<? super I, Q> next,
            ThrowableBiFunction<? super O, ? super Q, ? extends R> combiner) {
        return input -> {
            Processed<O> result = process(input);
            if (result.isError()) {
                return Processed.error(result.exception());
            }
            if (result.isDone()) {
                Processed<Q> nextResult = next.process(input);
                if (nextResult.isError()) {
                    return Processed.error(nextResult.exception());
                }
                if (nextResult.isDone()) {
                    try {
                        return Processed.done(combiner.apply(result.value(), nextResult.value()));
                    } catch (Exception exception) {
                        return Processed.error(exception);
                    }
                }
            }
            return Processed.skipped();
        };
    }

    /**
     * Turns {@link Processed#error(Exception)} into {@link Processed#done(Object)}. {@code
     * recovery} receives the original input and the exception and its value, including {@code null},
     * is the payload. Done and skip are returned unchanged and {@code recovery} is not called. An
     * exception from {@code recovery} is {@link Processed#error(Exception)}. {@link Error} is not
     * caught.
     */
    default Processor<I, O> recover(ThrowableFunction<Failure<I>, O> recovery) {
        return input -> {
            Processed<O> result = process(input);
            if (result.isError()) {
                try {
                    return Processed.done(recovery.apply(new Failure<>(input, result.exception())));
                } catch (Exception exception) {
                    return Processed.error(exception);
                }
            }
            return result;
        };
    }

    /**
     * Keeps a successful output only if {@code predicate} holds; otherwise {@link
     * Processed#skipped()}. Skip/error from this processor propagate unchanged. Delegates to
     * {@link Processed#filter}: an exception from {@code predicate} becomes {@link
     * Processed#error(Exception)}. {@code when} is the same check on the input and starts a
     * chain.
     */
    default Processor<I, O> filter(Predicate<? super O> predicate) {
        return input -> process(input).filter(predicate);
    }

    /** Continues with a handler ({@link Processed#thenHandle}). */
    default Handler<I> thenHandle(Handler<? super O> handler) {
        return input -> process(input).thenHandle(handler::handle);
    }

    /** Identifies this processor by a constant routing key. */
    default <K> IdentifiedProcessor<K, I, O> identifiedBy(K identifier) {
        return new ConstantIdentifiedProcessor<>(identifier, this);
    }

    /**
     * Bare processor from a {@link ThrowableFunction} ({@code null} result → {@link Processed#done}).
     * A thrown exception is {@link Processed#error(Exception)}.
     */
    static <A, B> Processor<A, B> of(ThrowableFunction<A, B> transform) {
        return new FunctionProcessor<>(notNull(transform, "transform"));
    }

    /**
     * Starts a chain from the input. The input is {@link Processed#done(Object) done} with that same
     * value when {@code predicate} holds, and {@link Processed#skipped()} otherwise. An exception
     * from the predicate is {@link Processed#error(Exception)}. The instance {@link #filter} checks
     * a successful output of an existing chain.
     */
    static <A> Processor<A, A> when(Predicate<? super A> predicate) {
        return new FilterProcessor<>(notNull(predicate, "predicate"));
    }
}
