package ru.operation.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Tries handlers in order on the same input until one returns {@link Handled#done()} or {@link
 * Handled#error(Exception)}; otherwise returns {@link Handled#skipped()} when every step skipped.
 * Built via {@link Handler#or}; nested {@code or} chains flatten into one list. Exceptions thrown
 * by a delegate propagate to the caller (no mapping to {@link Handled#error(Exception)}).
 */
final class OrHandler<I> implements Handler<I> {

    private final Logger log = getLogger(getClass());
    private final List<Handler<? super I>> steps;

    OrHandler(Handler<? super I> left, Handler<? super I> right) {
        this.steps = new ArrayList<>();
        if (left instanceof OrHandler) {
            @SuppressWarnings("unchecked")
            OrHandler<I> orLeft = (OrHandler<I>) left;
            this.steps.addAll(orLeft.steps);
        } else {
            this.steps.add(left);
        }
        this.steps.add(notNull(right, "next"));
    }

    @Override
    public Handled handle(I input) {
        log.trace("Handling input '{}'", input);
        for (Handler<? super I> handler : steps) {
            Handled handled = handler.handle(input);
            log.trace("Handler '{}' has result '{}'", handler.getClass().getSimpleName(), handled);
            if (handled.isError() || handled.isDone()) {
                log.trace("Handled input '{}' with result {}", input, handled);
                return handled;
            }
        }
        Handled skipped = Handled.skipped();
        log.trace("Handled input '{}' with result {}", input, skipped);
        return skipped;
    }

    @Override
    public String toString() {
        return "Or(" + steps + ")";
    }
}
