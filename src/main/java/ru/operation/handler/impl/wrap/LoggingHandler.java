package ru.operation.handler.impl.wrap;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.isNullOrEmpty;
import static ru.operation.util.Checks.notNull;

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
        this.delegate = notNull(delegate, "delegate");
        this.level = notNull(level, "level");
        this.log =
                isNullOrEmpty(name)
                        ? getLogger(delegate.getClass())
                        : getLogger(delegate.getClass().getName() + "[" + name + "]");
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
