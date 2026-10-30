package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.slf4j.event.Level;
import ru.operation.Failed;
import ru.operation.MdcEntry;
import ru.operation.handler.Handler;
import ru.operation.handler.impl.PropagatingHandler;
import ru.operation.processor.impl.AroundProcessor;
import ru.operation.processor.impl.CatchingProcessor;
import ru.operation.processor.impl.LoggingExceptionProcessor;
import ru.operation.processor.impl.MdcProcessor;
import ru.operation.processor.impl.WhenProcessor;
import ru.operation.processor.impl.wrap.LoggingProcessor;
import ru.operation.util.function.ThrowableConsumer;
import ru.operation.util.function.ThrowablePredicate;

/**
 * Builds a processor stack with a fixed wrap order on {@link #build()}: {@code
 * Mdc(Logging(Catching(When(Around(core)))))}. MDC wraps logging so log lines see MDC values.
 * Catching always converts {@link Exception} to {@link Processed} ERROR (or custom {@link
 * #onException}). {@link #when} sits inside catching and outside around, so a failed predicate
 * skips before/after and the core. When, around, logging and MDC are omitted when not configured.
 */
public final class ProcessorCompose<I, O> {

    private final Processor<I, O> core;
    private String name;
    private Level loggingLevel;
    private Handler<I> beforeHandler;
    private Handler<Processed<O>> afterHandler;
    private Processor<Failed<I>, O> onExceptionProcessor;
    private ThrowablePredicate<I> when;
    private final List<MdcEntry<I>> mdcEntries = new ArrayList<>();

    private ProcessorCompose(@NonNull Processor<I, O> core) {
        this.core = core;
    }

    public static <I, O> ProcessorCompose<I, O> from(Processor<I, O> core) {
        return new ProcessorCompose<>(core);
    }

    /** Optional logger suffix. The logger is the core's class. Absent means no suffix. */
    public ProcessorCompose<I, O> named(String name) {
        this.name = notNull(name, "name");
        return this;
    }

    public ProcessorCompose<I, O> logging() {
        return logging(Level.DEBUG);
    }

    public ProcessorCompose<I, O> logging(Level level) {
        this.loggingLevel = notNull(level, "logging level");
        return this;
    }

    /**
     * Skips before, after and the core when {@code predicate} is false ({@link
     * Processed#skipped()}). A later call replaces the previous predicate. A predicate exception,
     * including a checked one, reaches {@link #onException} as the original type. This is an input
     * guard; {@link Processor#filter} checks a successful output.
     */
    public ProcessorCompose<I, O> when(ThrowablePredicate<I> predicate) {
        this.when = notNull(predicate, "predicate");
        return this;
    }

    /**
     * Runs before the core. {@code DONE} and {@code SKIPPED} do not change the core result. {@code
     * ERROR} skips the core and after and becomes the operation result. A thrown exception reaches
     * {@link #onException}.
     */
    public ProcessorCompose<I, O> before(Handler<I> before) {
        this.beforeHandler = notNull(before, "before handler");
        return this;
    }

    public ProcessorCompose<I, O> before(ThrowableConsumer<I> before) {
        return before(new PropagatingHandler<>(notNull(before, "before consumer")));
    }

    /**
     * Observes the main {@link Processed}. {@code DONE} and {@code SKIPPED} do not replace it.
     * {@code ERROR} becomes the operation result. If the main result is already an error, that
     * exception is suppressed on this failure. A thrown exception reaches {@link #onException}.
     */
    public ProcessorCompose<I, O> after(Handler<Processed<O>> after) {
        this.afterHandler = notNull(after, "after handler");
        return this;
    }

    public ProcessorCompose<I, O> after(ThrowableConsumer<Processed<O>> after) {
        return after(new PropagatingHandler<>(notNull(after, "after consumer")));
    }

    /**
     * Fully replaces default exception mapping. Accepts a lambda ({@link Processor} is a functional
     * interface). May recover to a successful {@link Processed} or return {@link
     * Processed#error(Exception)}.
     */
    public ProcessorCompose<I, O> onException(Processor<Failed<I>, O> onException) {
        this.onExceptionProcessor = notNull(onException, "onException");
        return this;
    }

    public <T> ProcessorCompose<I, O> mdc(String key, Function<I, T> valueExtractor) {
        mdcEntries.add(MdcEntry.of(key, valueExtractor));
        return this;
    }

    public ProcessorCompose<I, O> mdc(MdcEntry<I> entry) {
        mdcEntries.add(notNull(entry, "mdc entry"));
        return this;
    }

    public ProcessorCompose<I, O> mdc(List<MdcEntry<I>> entries) {
        mdcEntries.addAll(notNull(entries, "mdc entries"));
        return this;
    }

    public Processor<I, O> build() {
        Processor<Failed<I>, O> onException =
                onExceptionProcessor != null
                        ? onExceptionProcessor
                        : new LoggingExceptionProcessor<>();

        Processor<I, O> result = core;
        if (beforeHandler != null || afterHandler != null) {
            result = new AroundProcessor<>(beforeHandler, result, afterHandler);
        }
        if (when != null) {
            result = new WhenProcessor<>(when, result);
        }
        result = new CatchingProcessor<>(result, onException);
        if (loggingLevel != null) {
            result = new LoggingProcessor<>(result, core.getClass(), name, loggingLevel);
        }
        if (!mdcEntries.isEmpty()) {
            result = new MdcProcessor<>(mdcEntries, result);
        }
        return result;
    }
}
