package ru.operation.processor.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@Getter
@RequiredArgsConstructor
public class IdentifiedByConstProcessor<K, I, O> implements IdentifiedProcessor<K, I, O> {

    @NonNull private final K identifier;
    @NonNull private final Processor<I, O> delegate;

    @Override
    public K getIdentifier() {
        return identifier;
    }

    @Override
    public Processed<O> process(I input) {
        return delegate.process(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
