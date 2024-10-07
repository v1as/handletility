package ru.v1as.processor.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.processor.Processed.error;

import org.slf4j.Logger;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

public class LoggingExceptionProcessor<O> implements Processor<Exception, O> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Processed<O> process(Exception exception) {
        log.warn("Processing error", exception);
        return error(exception);
    }
}