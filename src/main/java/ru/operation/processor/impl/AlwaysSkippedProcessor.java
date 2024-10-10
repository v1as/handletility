package ru.operation.processor.impl;

import static ru.operation.processor.Processed.skipped;

import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class AlwaysSkippedProcessor implements Processor<Object, Object> {

    private static final AlwaysSkippedProcessor INSTANCE = new AlwaysSkippedProcessor();

    @Override
    public Processed<Object> process(Object input) {
        return skipped();
    }

    @SuppressWarnings("unchecked")
    public static <I, O> Processor<I, O> alwaysSkippedProcessor() {
        return (Processor<I, O>) INSTANCE;
    }
}
