package ru.operation.handler.impl;

import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor
public class ProcessorHandler<I, O> implements Handler<I> {

    @NonNull private final Processor<I, O> processor;

    @Override
    public Handled handle(I input) {
        Processed<O> processed = processor.process(input);
        if (processed.isError()) {
            return error(processed.exception());
        }
        if (processed.isSkipped()) {
            return skipped();
        }
        return done();
    }
}
