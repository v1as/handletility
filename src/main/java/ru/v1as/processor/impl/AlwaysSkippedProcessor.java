package ru.v1as.processor.impl;

import static ru.v1as.processor.Processed.skipped;

import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

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
