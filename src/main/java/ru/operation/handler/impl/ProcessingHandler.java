package ru.operation.handler.impl;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.handled;
import static ru.operation.handler.Handled.skipped;

import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

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
