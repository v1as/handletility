package ru.operation.processor;

import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.event.Level;
import ru.operation.Failed;
import ru.operation.MdcEntry;
import ru.operation.handler.Handler;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.processor.impl.AroundProcessor;
import ru.operation.processor.impl.CatchingProcessor;
import ru.operation.processor.impl.IdentifiedByConstProcessor;
import ru.operation.processor.impl.LoggingExceptionProcessor;
import ru.operation.processor.impl.MdcProcessor;
import ru.operation.processor.impl.wrap.LoggingProcessor;

/**
 * Builds a processor stack with a fixed wrap order on {@link #build()}: {@code
 * Identified(Mdc(Logging(Catching(Around(core)))))}. MDC wraps logging so log lines see MDC values.
 * Catching always converts {@link Exception} to {@link Processed} ERROR (or custom {@link
 * #onException}). {@link #identified} is required.
 */
public final class ProcessorCompose<I, O> {

    private final Processor<I, O> core;
    private Object identifier;
    private String name;
    private Level loggingLevel;
    private Handler<I> beforeHandler;
    private Handler<Processed<O>> afterHandler;
    private Processor<Failed<I>, O> onExceptionProcessor;
    private final List<MdcEntry<I>> mdcEntries = new ArrayList<>();

    private ProcessorCompose(Processor<I, O> core) {
        this.core = core;
    }

    public static <I, O> ProcessorCompose<I, O> from(Processor<I, O> core) {
        return new ProcessorCompose<>(core);
    }

    public <K> ProcessorCompose<I, O> identified(K identifier) {
        this.identifier = notNull(identifier, "identifier");
        return this;
    }

    /** Optional log/display name; if absent, {@link #identified} value is used for logging. */
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

    public ProcessorCompose<I, O> before(Handler<I> before) {
        this.beforeHandler = notNull(before, "before handler");
        return this;
    }

    public ProcessorCompose<I, O> before(Consumer<I> before) {
        return before(new SimpleHandler<>(notNull(before, "before consumer")::accept));
    }

    /** Observes the main {@link Processed} result without replacing it. */
    public ProcessorCompose<I, O> after(Handler<Processed<O>> after) {
        this.afterHandler = notNull(after, "after handler");
        return this;
    }

    public ProcessorCompose<I, O> after(Consumer<Processed<O>> after) {
        return after(new SimpleHandler<>(notNull(after, "after consumer")::accept));
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

    @SuppressWarnings("unchecked")
    public <K> IdentifiedProcessor<K, I, O> build() {
        K id = (K) notNull(identifier, "identifier");
        String logName = name != null ? name : String.valueOf(id);
        Processor<Failed<I>, O> onException =
                onExceptionProcessor != null
                        ? onExceptionProcessor
                        : new LoggingExceptionProcessor<>();

        Processor<I, O> result = core;
        if (beforeHandler != null || afterHandler != null) {
            result = new AroundProcessor<>(beforeHandler, result, afterHandler);
        }
        result = new CatchingProcessor<>(result, onException);
        if (loggingLevel != null) {
            result = new LoggingProcessor<>(result, logName, loggingLevel);
        }
        if (!mdcEntries.isEmpty()) {
            result = new MdcProcessor<>(mdcEntries, result);
        }
        return new IdentifiedByConstProcessor<>(id, result);
    }
}
