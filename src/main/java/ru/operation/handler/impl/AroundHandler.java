package ru.operation.handler.impl;

import static ru.operation.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.Builder;
import lombok.NonNull;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/**
 * AOP-style around: {@code before(input)} → {@code handler(input)} → {@code after(result)}. {@code
 * DONE} and {@code SKIPPED} from a hook do not replace the main result, and a skipped before still
 * runs the core. {@code ERROR} from a hook is returned as the operation result: a failed before
 * skips the core and after; a failed after replaces a successful main result. When the main result
 * is already an error, that exception is suppressed on the after-hook failure. An exception thrown
 * by a hook propagates unchanged.
 */
@Builder
public class AroundHandler<I> implements Handler<I> {

    private final Handler<I> beforeHandler;
    private final Handler<I> handler;
    private final Handler<Handled> afterHandler;

    public AroundHandler(
            Handler<I> beforeHandler, @NonNull Handler<I> handler, Handler<Handled> afterHandler) {
        this.beforeHandler = beforeHandler != null ? beforeHandler : alwaysSkippedHandler();
        this.handler = handler;
        this.afterHandler = afterHandler != null ? afterHandler : alwaysSkippedHandler();
    }

    @Override
    public Handled handle(I input) {
        Handled before = beforeHandler.handle(input);
        if (before.isError()) {
            return before;
        }
        Handled result = handler.handle(input);
        Handled after = afterHandler.handle(result);
        if (after.isError()) {
            if (result.isError()) {
                suppressCoreFailure(after.exception(), result.exception());
            }
            return after;
        }
        return result;
    }

    private static void suppressCoreFailure(Exception hookFailure, Exception coreFailure) {
        if (hookFailure != coreFailure) {
            hookFailure.addSuppressed(coreFailure);
        }
    }

    @Override
    public String toString() {
        return "Around(before="
                + beforeHandler
                + ", handler="
                + handler
                + ", after="
                + afterHandler
                + ")";
    }
}
