package ru.operation.handler.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.handler.Handled.error;

import org.slf4j.Logger;
import ru.operation.Failure;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class LoggingExceptionHandler<I> implements Handler<Failure<I>> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Handled handle(Failure<I> failure) {
        log.warn("Error while handling '{}'", failure.input(), failure.exception());
        return error(failure.exception());
    }
}
