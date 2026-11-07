package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.isNullOrEmpty;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Decorator that logs processor outcome. {@link Processed#skipped()} is logged at {@link
 * Level#TRACE}; done and error use the configured level (default {@link Level#DEBUG}).
 */
final class LoggingProcessor<I, O> implements Processor<I, O> {

    private final Processor<I, O> delegate;
    private final Logger log;
    private final Level level;

    public LoggingProcessor(Processor<I, O> delegate) {
        this(delegate, null, Level.DEBUG);
    }

    public LoggingProcessor(Processor<I, O> delegate, String name) {
        this(delegate, name, Level.DEBUG);
    }

    public LoggingProcessor(Processor<I, O> delegate, Level level) {
        this(delegate, null, level);
    }

    public LoggingProcessor(Processor<I, O> delegate, String name, Level level) {
        this(delegate, null, name, level);
    }

    /**
     * {@code loggerType} names the SLF4J logger; {@code null} uses the delegate's class. {@code
     * name} is an optional suffix.
     */
    public LoggingProcessor(
            @NonNull Processor<I, O> delegate,
            Class<?> loggerType,
            String name,
            @NonNull Level level) {
        this.delegate = delegate;
        this.level = level;
        Class<?> type = loggerType != null ? loggerType : delegate.getClass();
        this.log =
                isNullOrEmpty(name)
                        ? getLogger(type)
                        : getLogger(type.getName() + "[" + name + "]");
    }

    @Override
    public Processed<O> process(I input) {
        Processed<O> processed = delegate.process(input);
        Level outcomeLevel = processed.isSkipped() ? Level.TRACE : level;
        log.atLevel(outcomeLevel).log("Processed '{}' to '{}'", input, processed);
        return processed;
    }

    @Override
    public String toString() {
        return "Logging(" + level + ", " + delegate + ")";
    }
}
