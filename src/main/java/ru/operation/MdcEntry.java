package ru.operation;

import static ru.operation.util.Checks.notNull;

import java.util.function.Function;
import ru.operation.processor.Processor;

/** Binding of an MDC key to a value extracted from the input. */
public final class MdcEntry<I> {

    private final String key;
    private final Processor<I, ?> valueProcessor;

    public MdcEntry(String key, Processor<I, ?> valueProcessor) {
        notNull(key, "mdc key");
        notNull(valueProcessor, "mdc value processor");
        this.key = key;
        this.valueProcessor = valueProcessor;
    }

    public String key() {
        return key;
    }

    public Processor<I, ?> valueProcessor() {
        return valueProcessor;
    }

    /** Preferred factory: method references like {@code Request::id} work without wrapping. */
    public static <I, T> MdcEntry<I> of(String key, Function<I, T> valueExtractor) {
        return new MdcEntry<>(
                key, Processor.of(notNull(valueExtractor, "mdc value extractor")::apply));
    }
}
