package ru.v1as.processor.impl;

import lombok.RequiredArgsConstructor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@RequiredArgsConstructor
public class ConstProcessor<I, O> implements Processor<I, O> {

    private final O value;

    @Override
    public Processed<O> process(I input) {
        return Processed.processed(value);
    }
}
