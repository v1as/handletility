package ru.v1as.processor.impl;

import lombok.RequiredArgsConstructor;
import ru.v1as.processor.IdentifiedProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@RequiredArgsConstructor
public class IdentifiedByConstProcessor<K, I, O> implements IdentifiedProcessor<K, I, O> {

    private final K identifier;
    private final Processor<I, O> processor;

    @Override
    public K getIdentifier() {
        return identifier;
    }

    @Override
    public Processed<O> process(I input) {
        return processor.process(input);
    }
}
