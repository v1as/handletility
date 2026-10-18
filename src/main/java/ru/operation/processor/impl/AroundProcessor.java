package ru.operation.processor.impl;

import static ru.operation.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.Builder;
import lombok.NonNull;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * AOP-style around: {@code before(input)} → {@code processor(input)} → {@code after(result)}. Hooks
 * are {@link Handler}s (side effects). The returned {@link Processed} is always from the main
 * processor; after only observes it.
 */
@Builder
public class AroundProcessor<I, O> implements Processor<I, O> {

    private final Handler<I> beforeHandler;
    private final Processor<I, O> processor;
    private final Handler<Processed<O>> afterHandler;

    public AroundProcessor(
            Handler<I> beforeHandler,
            @NonNull Processor<I, O> processor,
            Handler<Processed<O>> afterHandler) {
        this.beforeHandler = beforeHandler != null ? beforeHandler : alwaysSkippedHandler();
        this.processor = processor;
        this.afterHandler = afterHandler != null ? afterHandler : alwaysSkippedHandler();
    }

    @Override
    public Processed<O> process(I input) {
        beforeHandler.handle(input);
        Processed<O> result = processor.process(input);
        afterHandler.handle(result);
        return result;
    }

    @Override
    public String toString() {
        return "Around(before="
                + beforeHandler
                + ", processor="
                + processor
                + ", after="
                + afterHandler
                + ")";
    }
}
