package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import org.slf4j.Logger;

public abstract class AbstractProcessor<I, O> implements Processor<I, O> {

    protected final Logger log = getLogger(this.getClass());

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
            log.trace("Processed '{}' to '{}'", input, processed);
            return processed;
        } catch (Exception ex) {
            return onException(input, ex);
        }
    }

    protected abstract Processed<O> processInternal(I input);

    protected Processed<O> onFailedCheck(I input) {
        log.trace("Skipped because of unsuitable input: '{}'", input);
        return skipped();
    }

    protected Processed<O> onException(I input, Exception ex) {
        log.warn("Processing error of '{}'", input, ex);
        return error(ex);
    }
}
