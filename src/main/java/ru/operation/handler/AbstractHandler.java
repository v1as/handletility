package ru.operation.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import org.slf4j.Logger;

/**
 * Template for handlers: optional {@link #check(Object)} and exception → {@link
 * ru.operation.ResultState#ERROR}. Naming and outcome logging belong in wrappers / compose.
 */
public abstract class AbstractHandler<T> implements Handler<T> {

    protected final Logger log = getLogger(getClass());

    public boolean check(T input) {
        return true;
    }

    @Override
    public Handled handle(T input) {
        try {
            if (!check(input)) {
                return onFailedCheck(input);
            }
            return handleInternal(input);
        } catch (Exception e) {
            return onException(input, e);
        }
    }

    protected abstract Handled handleInternal(T input) throws Exception;

    protected Handled onFailedCheck(T input) {
        log.trace("Skipped because of unsuitable input: '{}'", input);
        return skipped();
    }

    protected Handled onException(T input, Exception e) {
        log.warn("Error while handling '{}'", input, e);
        return error(e);
    }
}
