package ru.v1as.processor.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.processor.Processed.error;

import org.slf4j.Logger;
import ru.v1as.Failed;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

public class LoggingExceptionProcessor<I, O> implements Processor<Failed<I>, O> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Processed<O> process(Failed<I> failed) {
        log.warn("Processing error for {}", failed.value(), failed.exception());
        return error(failed.exception());
    }
}
