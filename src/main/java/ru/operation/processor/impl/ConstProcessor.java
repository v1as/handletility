package ru.operation.processor.impl;

import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor
public class ConstProcessor<I, O> implements Processor<I, O> {

    private final O value;

    @Override
    public Processed<O> process(I input) {
        return Processed.processed(value);
    }
}
