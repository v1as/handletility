package ru.operation.handler;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import ru.operation.AbstractOperation;

public abstract class AbstractHandler<T> extends AbstractOperation implements Handler<T> {

    public boolean check(T input) {
        return true;
    }

    @Override
    public Handled handle(T input) {
        try {
            if (!check(input)) {
                return onFailedCheck(input);
            }
            Handled handled = handleInternal(input);
            logHandled(input, handled);
            return handled;
        } catch (Exception e) {
            return onException(input, e);
        }
    }

    private void logHandled(T input, Handled handled) {
        if (log.isTraceEnabled()) {
            log.trace("Handled input '{}' with result {}", input, handled);
        } else if (nameDefined) {
            log.debug("Handled input with result {}", handled);
        }
    }

    protected abstract Handled handleInternal(T input) throws Exception;

    protected Handled onFailedCheck(T input) {
        logFailedCheck(input);
        return skipped();
    }

    private void logFailedCheck(T input) {
        if (log.isTraceEnabled()) {
            log.trace("Skipped because of unsuitable input: '{}'", input);
        } else if (nameDefined) {
            log.debug("Skipped because of unsuitable input.");
        }
    }

    protected Handled onException(T input, Exception e) {
        log.warn("Error while handling '{}'", input, e);
        return error(e);
    }
}
