package ru.operation.handler;

import static lombok.AccessLevel.PRIVATE;
import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import ru.operation.Failure;
import ru.operation.MdcEntry;
import ru.operation.handler.impl.AroundHandler;
import ru.operation.handler.impl.CatchingHandler;
import ru.operation.handler.impl.LoggingExceptionHandler;
import ru.operation.handler.impl.MdcHandler;
import ru.operation.handler.impl.PropagatingHandler;
import ru.operation.handler.impl.WhenHandler;
import ru.operation.handler.impl.wrap.LoggingHandler;
import ru.operation.util.function.ThrowableConsumer;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Builds a handler stack with a fixed wrap order on {@link #build()}: {@code
 * Mdc(Logging(Catching(When(Around(core)))))}. MDC wraps logging so log lines see MDC values.
 * Catching always converts {@link Exception} to {@link Handled} ERROR (or custom {@link
 * #onException}). {@link #when} sits inside catching and outside around, so a failed predicate
 * skips before/after and the core. When, around, logging and MDC are omitted when not configured.
 */
@RequiredArgsConstructor(access = PRIVATE)
public final class HandlerCompose<I> {

    @NonNull private final Handler<I> core;
    private String name;
    private Level loggingLevel;
    private Handler<I> beforeHandler;
    private Handler<Handled> afterHandler;
    private Handler<Failure<I>> onException;
    private ThrowablePredicate<I> when;
    private final List<MdcEntry<I>> mdcEntries = new ArrayList<>();

    public static <I> HandlerCompose<I> from(Handler<I> core) {
        return new HandlerCompose<>(core);
    }

    /** Optional logger suffix. The logger is the core's class. Absent means no suffix. */
    public HandlerCompose<I> named(String name) {
        this.name = notNull(name, "name");
        return this;
    }

    public HandlerCompose<I> logging() {
        return logging(Level.DEBUG);
    }

    public HandlerCompose<I> logging(Level level) {
        this.loggingLevel = notNull(level, "logging level");
        return this;
    }

    /**
     * Skips before, after and the core when {@code predicate} is false ({@link Handled#skipped()}).
     * A later call replaces the previous predicate. A predicate exception, including a checked one,
     * reaches {@link #onException} as the original type.
     */
    public HandlerCompose<I> when(ThrowablePredicate<I> predicate) {
        this.when = notNull(predicate, "predicate");
        return this;
    }

    /**
     * Runs before the core. {@code DONE} and {@code SKIPPED} do not change the core result. {@code
     * ERROR} skips the core and after and becomes the operation result. A thrown exception reaches
     * {@link #onException}.
     */
    public HandlerCompose<I> before(Handler<I> before) {
        this.beforeHandler = notNull(before, "before handler");
        return this;
    }

    public HandlerCompose<I> before(ThrowableConsumer<I> before) {
        return before(new PropagatingHandler<>(notNull(before, "before consumer")));
    }

    /**
     * Observes the main {@link Handled}. {@code DONE} and {@code SKIPPED} do not replace it. {@code
     * ERROR} becomes the operation result. If the main result is already an error, that exception
     * is suppressed on this failure. A thrown exception reaches {@link #onException}.
     */
    public HandlerCompose<I> after(Handler<Handled> after) {
        this.afterHandler = notNull(after, "after handler");
        return this;
    }

    public HandlerCompose<I> after(ThrowableConsumer<Handled> after) {
        return after(new PropagatingHandler<>(notNull(after, "after consumer")));
    }

    /**
     * Fully replaces default exception mapping. May recover to {@link Handled#done()} or return
     * {@link Handled#error(Exception)}.
     */
    public HandlerCompose<I> onException(Handler<Failure<I>> onException) {
        this.onException = notNull(onException, "onException");
        return this;
    }

    public <T> HandlerCompose<I> mdc(String key, Function<I, T> valueExtractor) {
        mdcEntries.add(MdcEntry.of(key, valueExtractor));
        return this;
    }

    public HandlerCompose<I> mdc(MdcEntry<I> entry) {
        mdcEntries.add(notNull(entry, "mdc entry"));
        return this;
    }

    public HandlerCompose<I> mdc(List<MdcEntry<I>> entries) {
        mdcEntries.addAll(notNull(entries, "mdc entries"));
        return this;
    }

    public Handler<I> build() {
        Handler<Failure<I>> exceptionMapping =
                this.onException != null ? this.onException : new LoggingExceptionHandler<>();

        Handler<I> result = core;
        if (beforeHandler != null || afterHandler != null) {
            result = new AroundHandler<>(beforeHandler, result, afterHandler);
        }
        if (when != null) {
            result = new WhenHandler<>(when, result);
        }
        result = new CatchingHandler<>(result, exceptionMapping);
        if (loggingLevel != null) {
            result = new LoggingHandler<>(result, core.getClass(), name, loggingLevel);
        }
        if (!mdcEntries.isEmpty()) {
            result = new MdcHandler<>(mdcEntries, result);
        }
        return result;
    }
}
