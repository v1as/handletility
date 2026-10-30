package ru.operation.processor.impl;

import static ru.operation.handler.impl.AlwaysSkippedHandler.alwaysSkippedHandler;

import lombok.Builder;
import lombok.NonNull;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * AOP-style around: {@code before(input)} → {@code processor(input)} → {@code after(result)}. Hooks
 * are {@link Handler}s (side effects). {@code DONE} and {@code SKIPPED} from a hook do not replace
 * the main result, and a skipped before still runs the core. {@code ERROR} from a hook is returned
 * as {@link Processed#error(Exception)}: a failed before skips the core and after; a failed after
 * replaces a successful main result. When the main result is already an error, that exception is
 * suppressed on the after-hook failure. An exception thrown by a hook propagates unchanged.
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
        Handled before = beforeHandler.handle(input);
        if (before.isError()) {
            return Processed.error(before.exception());
        }
        Processed<O> result = processor.process(input);
        Handled after = afterHandler.handle(result);
        if (after.isError()) {
            if (result.isError()) {
                suppressCoreFailure(after.exception(), result.exception());
            }
            return Processed.error(after.exception());
        }
        return result;
    }

    private static void suppressCoreFailure(Exception hookFailure, Exception coreFailure) {
        if (hookFailure == null || coreFailure == null || hookFailure == coreFailure) {
            return;
        }
        hookFailure.addSuppressed(coreFailure);
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
