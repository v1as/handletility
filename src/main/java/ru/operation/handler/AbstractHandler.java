package ru.operation.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import org.slf4j.Logger;

/**
 * Template for handlers: optional {@link #check(Object)}; when it returns {@code false}, {@link
 * #onCheckRejected} runs (default {@link Handled#skipped()}). Thrown exceptions → {@link
 * ru.operation.ResultState#ERROR}. Naming and outcome logging belong in wrappers / builder.
 */
public abstract class AbstractHandler<I> implements Handler<I> {

    protected final Logger log = getLogger(getClass());

    /** Gate before {@link #handleInternal}; {@code false} invokes {@link #onCheckRejected}. */
    public boolean check(I input) {
        return true;
    }

    @Override
    public Handled handle(I input) {
        try {
            if (!check(input)) {
                return onCheckRejected(input);
            }
            return handleInternal(input);
        } catch (Exception exception) {
            return onException(input, exception);
        }
    }

    protected abstract Handled handleInternal(I input) throws Exception;

    /** Outcome when {@link #check} returns {@code false}; default is skip. */
    protected Handled onCheckRejected(I input) {
        log.trace("Skipped because check rejected input: '{}'", input);
        return skipped();
    }

    protected Handled onException(I input, Exception exception) {
        log.warn("Error while handling '{}'", input, exception);
        return error(exception);
    }
}
