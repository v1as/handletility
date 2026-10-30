package ru.operation.handler.impl.wrap;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.isNullOrEmpty;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/** Decorator that logs handler outcome at a configurable level (default {@link Level#DEBUG}). */
public class LoggingHandler<I> implements Handler<I> {

    private final Handler<I> delegate;
    private final Logger log;
    private final Level level;

    public LoggingHandler(Handler<I> delegate) {
        this(delegate, null, Level.DEBUG);
    }

    public LoggingHandler(Handler<I> delegate, String name) {
        this(delegate, name, Level.DEBUG);
    }

    public LoggingHandler(Handler<I> delegate, Level level) {
        this(delegate, null, level);
    }

    public LoggingHandler(Handler<I> delegate, String name, Level level) {
        this(delegate, null, name, level);
    }

    /**
     * {@code loggerType} names the SLF4J logger; {@code null} uses the delegate's class. {@code
     * name} is an optional suffix.
     */
    public LoggingHandler(
            @NonNull Handler<I> delegate, Class<?> loggerType, String name, @NonNull Level level) {
        this.delegate = delegate;
        this.level = level;
        Class<?> type = loggerType != null ? loggerType : delegate.getClass();
        this.log =
                isNullOrEmpty(name)
                        ? getLogger(type)
                        : getLogger(type.getName() + "[" + name + "]");
    }

    @Override
    public Handled handle(I input) {
        Handled handled = delegate.handle(input);
        log.atLevel(level).log("Handled input '{}' with result {}", input, handled);
        return handled;
    }

    @Override
    public String toString() {
        return "Logging(" + level + ", " + delegate + ")";
    }
}
