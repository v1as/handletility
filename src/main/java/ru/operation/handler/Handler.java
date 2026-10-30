package ru.operation.handler;

import ru.operation.handler.impl.IdentifiedByConstHandler;

/**
 * Side-effect operation over an input. Prefer extending {@link AbstractHandler} for check,
 * exception handling and logging.
 *
 * <p>{@link #or} tries the next handler on the same input when this one returns {@link
 * Handled#skipped()} and stops on done or error.
 */
@FunctionalInterface
public interface Handler<I> {

    Handled handle(I input);

    /**
     * Tries {@code next} on the same input when this handler returns {@link Handled#skipped()}.
     * {@code DONE} and {@code ERROR} are returned unchanged and {@code next} is not called. A chain
     * stops at the first done or error and stays skipped only when every step skips.
     */
    default Handler<I> or(Handler<? super I> next) {
        return input -> {
            Handled result = handle(input);
            return result.isSkipped() ? next.handle(input) : result;
        };
    }

    /** Identifies this handler by a constant routing key. */
    default <K> IdentifiedHandler<K, I> identifiedBy(K identifier) {
        return new IdentifiedByConstHandler<>(identifier, this);
    }
}
