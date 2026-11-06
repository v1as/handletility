package ru.operation.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.handler.Handled.error;

import org.slf4j.Logger;
import ru.operation.Failure;

final class LoggingExceptionHandler<I> implements Handler<Failure<I>> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Handled handle(Failure<I> failure) {
        log.warn("Error while handling '{}'", failure.input(), failure.exception());
        return error(failure.exception());
    }
}
