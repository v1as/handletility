package ru.operation.processor;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import ru.operation.AbstractOperation;

public abstract class AbstractProcessor<I, O> extends AbstractOperation implements Processor<I, O> {

    protected boolean check(I input) {
        return true;
    }

    @Override
    public Processed<O> process(I input) {
        try {
            if (!check(input)) {
                return onFailedCheck(input);
            }
            Processed<O> processed = processInternal(input);
            logProcessed(input, processed);
            return processed;
        } catch (Exception ex) {
            return onException(input, ex);
        }
    }

    private void logProcessed(I input, Processed<O> processed) {
        if (log.isTraceEnabled()) {
            log.trace("Processed '{}' to '{}'", input, processed);
        } else if (nameDefined) {
            log.debug("Processed with result {}", processed.state());
        }
    }

    protected abstract Processed<O> processInternal(I input);

    protected Processed<O> onFailedCheck(I input) {
        logOnFailedCheck(input);

        return skipped();
    }

    private void logOnFailedCheck(I input) {
        if (log.isTraceEnabled()) {
            log.trace("Skipped because of unsuitable input: '{}'", input);
        } else if (nameDefined) {
            log.debug("Skipped because of unsuitable input.");
        }
    }

    protected Processed<O> onException(I input, Exception ex) {
        log.warn("Processing error of '{}'", input, ex);
        return error(ex);
    }
}
