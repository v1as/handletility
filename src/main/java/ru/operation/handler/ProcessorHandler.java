package ru.operation.handler;

import static lombok.AccessLevel.PACKAGE;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor(access = PACKAGE)
final class ProcessorHandler<I, O> implements Handler<I> {

    @NonNull private final Processor<I, O> processor;

    @Override
    public Handled handle(I input) {
        Processed<O> processed = processor.process(input);
        if (processed.isError()) {
            return error(processed.exception());
        }
        if (processed.isDone()) {
            return done();
        }
        return skipped();
    }
}
