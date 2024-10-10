package ru.operation.processor.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;

import org.slf4j.Logger;
import ru.operation.Failed;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class LoggingExceptionProcessor<I, O> implements Processor<Failed<I>, O> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Processed<O> process(Failed<I> failed) {
        log.warn("Processing error for {}", failed.value(), failed.exception());
        return error(failed.exception());
    }
}
