package ru.operation.processor;

import static ru.operation.processor.Processed.error;
import static ru.operation.util.Checks.notNull;

import ru.operation.ProcessorOutcome;
import ru.operation.util.function.ThrowableConsumer;

final class AfterProcessor<I, O> implements Processor<I, O> {

    private final Processor<I, O> delegate;
    private final ThrowableConsumer<ProcessorOutcome<I, O>> next;

    AfterProcessor(Processor<I, O> delegate, ThrowableConsumer<ProcessorOutcome<I, O>> next) {
        this.delegate = delegate;
        this.next = notNull(next, "next");
    }

    @Override
    public Processed<O> process(I input) {
        Processed<O> main = delegate.process(input);
        try {
            next.accept(new ProcessorOutcome<>(input, main));
        } catch (Exception exception) {
            if (main.isError()) {
                suppressCoreFailure(exception, main.exception());
            }
            return error(exception);
        }
        return main;
    }

    private static void suppressCoreFailure(Exception hookFailure, Exception coreFailure) {
        if (hookFailure != coreFailure) {
            hookFailure.addSuppressed(coreFailure);
        }
    }

    @Override
    public String toString() {
        return "After(delegate=" + delegate + ", next=" + next + ")";
    }
}
