package ru.operation.handler;

import static ru.operation.handler.Handled.error;
import static ru.operation.util.Checks.notNull;

import ru.operation.HandlerOutcome;
import ru.operation.util.function.ThrowableConsumer;

final class AfterHandler<I> implements Handler<I> {

    private final Handler<I> delegate;
    private final ThrowableConsumer<HandlerOutcome<I>> next;

    AfterHandler(Handler<I> delegate, ThrowableConsumer<HandlerOutcome<I>> next) {
        this.delegate = delegate;
        this.next = notNull(next, "next");
    }

    @Override
    public Handled handle(I input) {
        Handled main = delegate.handle(input);
        try {
            next.accept(new HandlerOutcome<>(input, main));
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
