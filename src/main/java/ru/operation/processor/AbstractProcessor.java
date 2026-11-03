package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import org.slf4j.Logger;

/**
 * Template for processors: optional {@link #check(Object)}; when it returns {@code false}, {@link
 * #onCheckRejected} runs (default {@link Processed#skipped()}). Thrown exceptions → {@link
 * ru.operation.ResultState#ERROR}. Naming and outcome logging belong in wrappers / builder.
 */
public abstract class AbstractProcessor<I, O> implements Processor<I, O> {

    protected final Logger log = getLogger(getClass());

    /** Gate before {@link #processInternal}; {@code false} invokes {@link #onCheckRejected}. */
    protected boolean check(I input) {
        return true;
    }

    @Override
    public Processed<O> process(I input) {
        try {
            if (!check(input)) {
                return onCheckRejected(input);
            }
            return processInternal(input);
        } catch (Exception exception) {
            return onException(input, exception);
        }
    }

    protected abstract Processed<O> processInternal(I input) throws Exception;

    /** Outcome when {@link #check} returns {@code false}; default is skip. */
    protected Processed<O> onCheckRejected(I input) {
        log.trace("Skipped because check rejected input: '{}'", input);
        return skipped();
    }

    protected Processed<O> onException(I input, Exception exception) {
        log.warn("Processing error of '{}'", input, exception);
        return error(exception);
    }
}
