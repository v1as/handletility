package ru.operation;

import static ru.operation.util.Checks.notNull;

import java.util.function.Function;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.SimpleProcessor;

/** Binding of an MDC key to a value extracted from the input. */
public record MdcEntry<I>(String key, Processor<I, ?> valueProcessor) {

    public MdcEntry {
        notNull(key, "mdc key");
        notNull(valueProcessor, "mdc value processor");
    }

    /** Preferred factory: method references like {@code Request::id} work without wrapping. */
    public static <I, T> MdcEntry<I> of(String key, Function<I, T> valueExtractor) {
        return new MdcEntry<>(
                key, new SimpleProcessor<>(notNull(valueExtractor, "mdc value extractor")));
    }
}
