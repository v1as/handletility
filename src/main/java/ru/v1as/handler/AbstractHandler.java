package ru.v1as.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.handler.Handled.error;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import org.slf4j.Logger;

public abstract class AbstractHandler<T> implements Handler<T> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Handled handle(T input) {
        try {
            if (!check(input)) {
                log.trace("Skipped because of unsuitable input: '{}'", input);
                return skipped();
            }
            consume(input);
            log.trace("Handled input '{}'", input);
            return handled();
        } catch (Exception e) {
            log.warn("Error while handling '{}'", input, e);
            return error(e);
        }
    }

    protected boolean check(T input) {
        return true;
    }

    protected abstract void consume(T input);
}
