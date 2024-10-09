package ru.v1as.handler.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.v1as.Failed;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;

@RequiredArgsConstructor
public class LoggingExceptionHandler<I> implements Handler<Failed<I>> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Handled handle(Failed<I> failed) {
        log.warn("Handling error while processing {}", failed.value(), failed.exception());
        return error(failed.exception());
    }
}
