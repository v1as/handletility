package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import org.slf4j.Logger;

/**
 * Template for processors: optional {@link #check(Object)}; when it returns {@code false}, {@link
 * #onCheckRejected} runs (default {@link Processed#skipped()}). Thrown exceptions → {@link
 * ru.operation.ResultState#ERROR}. Check rejection and the result of {@link #processInternal}
 * are logged at trace. A chosen level and logger name stay on the logging wrapper.
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
            return onResult(input, processInternal(input));
        } catch (Exception exception) {
            return onException(input, exception);
        }
    }

    protected abstract Processed<O> processInternal(I input) throws Exception;

    /**
     * Logs the result of {@link #processInternal} at trace. An override may return a different
     * result.
     */
    protected Processed<O> onResult(I input, Processed<O> processed) {
        log.trace("Processed '{}' to '{}'", input, processed);
        return processed;
    }

    /** Outcome when {@link #check} returns {@code false}; default is skip. */
    protected Processed<O> onCheckRejected(I input) {
        log.trace("Skipped because check rejected input: '{}'", input);
        return skipped();
    }

    /** Logs at trace and returns the exception as {@link Processed#error(Exception)}. */
    protected Processed<O> onException(I input, Exception exception) {
        log.trace("Processing error of '{}'", input, exception);
        return error(exception);
    }
}
