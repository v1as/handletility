package ru.operation.processor.impl.wrap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/** Decorator that assigns a display name to a processor (for logging / toString). */
@RequiredArgsConstructor
public class NamedProcessor<I, O> implements Processor<I, O> {

    @Getter private final String name;
    private final Processor<I, O> delegate;

    @Override
    public Processed<O> process(I input) {
        return delegate.process(input);
    }

    @Override
    public String toString() {
        return "Named[" + name + "](" + delegate + ")";
    }
}
