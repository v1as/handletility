package ru.operation.handler.impl;

import static ru.operation.util.Checks.notNull;
import static ru.operation.util.Collections.copyNonEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.MDC;
import ru.operation.MdcEntry;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;

/** Decorator that sets one or more MDC keys for the duration of the delegate call. */
public class MdcHandler<I> implements Handler<I> {

    private final List<MdcEntry<I>> entries;
    private final Handler<I> handler;

    public MdcHandler(String mdcKey, Function<I, ?> mdcValueExtractor, Handler<I> handler) {
        this(List.of(MdcEntry.of(mdcKey, mdcValueExtractor)), handler);
    }

    public MdcHandler(List<MdcEntry<I>> entries, Handler<I> handler) {
        this.entries = copyNonEmpty(entries, "mdc entries");
        this.handler = notNull(handler, "handler");
    }

    @Override
    public Handled handle(I input) {
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
            return handler.handle(input);
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
        return "Mdc(" + keys + ", " + handler + ")";
    }
}
