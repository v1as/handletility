package ru.v1as.handler.impl;

import static ru.v1as.handler.Handled.error;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import lombok.RequiredArgsConstructor;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@RequiredArgsConstructor
public class ProcessingHandler<I, O> implements Handler<I> {

    private final Processor<I, O> processor;

    @Override
    public Handled handle(I input) {
        Processed<O> processed = processor.process(input);
        if (processed.isError()) {
            return error(processed.exception());
        }
        if (processed.isEmpty()) {
            return skipped();
        }
        return handled();
    }
}
