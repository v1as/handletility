package ru.operation.handler.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.operation.Failed;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

@RequiredArgsConstructor
public class LoggingExceptionHandler<I> implements Handler<Failed<I>> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Handled handle(Failed<I> failed) {
        log.warn("Handling error while processing {}", failed.value(), failed.exception());
        return error(failed.exception());
    }
}
