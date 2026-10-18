package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import org.slf4j.Logger;

/**
 * Template for processors: optional {@link #check(Object)} and exception → {@link
 * ru.operation.ResultState#ERROR}. Naming and outcome logging belong in wrappers / compose.
 */
public abstract class AbstractProcessor<I, O> implements Processor<I, O> {

    protected final Logger log = getLogger(getClass());

    protected boolean check(I input) {
        return true;
    }

    @Override
    public Processed<O> process(I input) {
        try {
            if (!check(input)) {
                return onFailedCheck(input);
            }
            return processInternal(input);
        } catch (Exception ex) {
            return onException(input, ex);
        }
    }

    protected abstract Processed<O> processInternal(I input) throws Exception;

    protected Processed<O> onFailedCheck(I input) {
        log.trace("Skipped because of unsuitable input: '{}'", input);
        return skipped();
    }

    protected Processed<O> onException(I input, Exception ex) {
        log.warn("Processing error of '{}'", input, ex);
        return error(ex);
    }
}
