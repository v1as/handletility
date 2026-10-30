package ru.operation.processor.impl;

import static ru.operation.util.Collections.copyNonEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.slf4j.MDC;
import ru.operation.MdcEntry;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/** Decorator that sets one or more MDC keys for the duration of the delegate call. */
public class MdcProcessor<I, O> implements Processor<I, O> {

    private final List<MdcEntry<I>> entries;
    private final Processor<I, O> delegate;

    public MdcProcessor(String key, Function<I, ?> valueExtractor, Processor<I, O> delegate) {
        this(List.of(MdcEntry.of(key, valueExtractor)), delegate);
    }

    public MdcProcessor(List<MdcEntry<I>> entries, @NonNull Processor<I, O> delegate) {
        this.entries = copyNonEmpty(entries, "mdc entries");
        this.delegate = delegate;
    }

    @Override
    public Processed<O> process(I input) {
        List<String> appliedKeys = new ArrayList<>(entries.size());
        List<String> previousValues = new ArrayList<>(entries.size());
        try {
            for (MdcEntry<I> entry : entries) {
                Processed<?> mdcValue = entry.valueProcessor().process(input);
                if (mdcValue.hasValue()) {
                    appliedKeys.add(entry.key());
                    previousValues.add(MDC.get(entry.key()));
                    MDC.put(entry.key(), String.valueOf(mdcValue.value()));
                }
            }
            return delegate.process(input);
        } finally {
            for (int i = appliedKeys.size() - 1; i >= 0; i--) {
                String previous = previousValues.get(i);
                if (previous != null) {
                    MDC.put(appliedKeys.get(i), previous);
                } else {
                    MDC.remove(appliedKeys.get(i));
                }
            }
        }
    }

    @Override
    public String toString() {
        List<String> keys = entries.stream().map(MdcEntry::key).toList();
        return "Mdc(" + keys + ", " + delegate + ")";
    }
}
