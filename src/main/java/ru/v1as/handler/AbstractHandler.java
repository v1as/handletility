package ru.v1as.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.handler.Handled.error;
import static ru.v1as.handler.Handled.skipped;

import org.slf4j.Logger;

public abstract class AbstractHandler<T> implements Handler<T> {

    protected final Logger log = getLogger(this.getClass());

    protected boolean check(T input) {
        return true;
    }

    @Override
    public Handled handle(T input) {
        try {
            if (!check(input)) {
                return onFailedCheck(input);
            }
            Handled handled = handleInternal(input);
            log.trace("Handled input '{}' with result '{}'", input, handled);
            return handled;
        } catch (Exception e) {
            return onException(input, e);
        }
    }

    protected abstract Handled handleInternal(T input);

    protected Handled onFailedCheck(T input) {
        log.trace("Skipped because of unsuitable input: '{}'", input);
        return skipped();
    }

    protected Handled onException(T input, Exception e) {
        log.warn("Error while handling '{}'", input, e);
        return error(e);
    }
}
