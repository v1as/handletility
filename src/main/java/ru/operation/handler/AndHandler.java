package ru.operation.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Runs handlers in order on the same input while each returns {@link Handled#done()}; skips and
 * errors stop the chain. Built via {@link Handler#and}; nested {@code and} chains flatten into one
 * list. Exceptions thrown by a delegate propagate to the caller.
 */
final class AndHandler<I> implements Handler<I> {

    private final Logger log = getLogger(getClass());
    private final List<Handler<? super I>> steps;

    AndHandler(Handler<? super I> left, Handler<? super I> right) {
        this.steps = new ArrayList<>();
        if (left instanceof AndHandler) {
            @SuppressWarnings("unchecked")
            AndHandler<I> andLeft = (AndHandler<I>) left;
            this.steps.addAll(andLeft.steps);
        } else {
            this.steps.add(left);
        }
        this.steps.add(notNull(right, "next"));
    }

    @Override
    public Handled handle(I input) {
        log.trace("Handling input '{}'", input);
        Handled last = null;
        for (Handler<? super I> handler : steps) {
            Handled handled = handler.handle(input);
            log.trace("Handler '{}' has result '{}'", handler.getClass().getSimpleName(), handled);
            if (handled.isError() || handled.isSkipped()) {
                log.trace("Handled input '{}' with result {}", input, handled);
                return handled;
            }
            last = handled;
        }
        Handled result = last != null ? last : Handled.done();
        log.trace("Handled input '{}' with result {}", input, result);
        return result;
    }

    @Override
    public String toString() {
        return "And(" + steps + ")";
    }
}
