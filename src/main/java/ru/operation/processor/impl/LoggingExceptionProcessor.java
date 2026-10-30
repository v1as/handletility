package ru.operation.processor.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;

import org.slf4j.Logger;
import ru.operation.Failure;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class LoggingExceptionProcessor<I, O> implements Processor<Failure<I>, O> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Processed<O> process(Failure<I> failure) {
        log.warn("Processing error for {}", failure.input(), failure.exception());
        return error(failure.exception());
    }
}
