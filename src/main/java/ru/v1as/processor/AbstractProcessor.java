package ru.v1as.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.processor.Processed.error;
import static ru.v1as.processor.Processed.processed;
import static ru.v1as.processor.Processed.skipped;

import org.slf4j.Logger;

public abstract class AbstractProcessor<I, O> implements Processor<I, O> {

    private final Logger log = getLogger(this.getClass());

    @Override
    public Processed<O> process(I input) {
        try {
            if (!check(input)) {
                log.trace("Skipped because of unsuitable input: '{}'", input);
                return skipped();
            }
            final O transformed = transform(input);
            log.trace("Transformed '{}' to: '{}'", input, transformed);
            if (transformed == null) {
                log.trace("Skipped '{}' because of null transformed", input);
                return skipped();
            }
            log.trace("Processed '{}' to '{}'", input, transformed);
            return processed(transformed);
        } catch (Exception ex) {
            log.warn("Processing error of '{}'", input, ex);
            return error(ex);
        }
    }

    protected boolean check(I input) {
        return true;
    }

    protected abstract O transform(I input);
}
