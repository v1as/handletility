package ru.operation.handler.impl.list;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.ok;
import static ru.operation.handler.Handled.skipped;
import static ru.operation.handler.impl.list.HandlerModifier.CONTINUE_ON_DONE;
import static ru.operation.handler.impl.list.HandlerModifier.CONTINUE_ON_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import ru.operation.Failed;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerException;
import ru.operation.handler.impl.LoggingExceptionHandler;

/**
 * Runs handlers in order. By default stops on the first {@code DONE} or {@code ERROR}; behaviour is
 * controlled by {@link HandlerModifier}.
 */
public class HandlerList<I> extends AbstractHandler<I> {

    private final List<? extends Handler<I>> handlers;
    private final Set<HandlerModifier> modifiers;
    private final Handler<Failed<I>> exceptionHandler;

    @Builder
    public HandlerList(
            @NonNull @Singular List<? extends Handler<I>> handlers,
            @Singular Set<HandlerModifier> modifiers,
            Handler<Failed<I>> exceptionHandler) {
        this.handlers = handlers;
        this.modifiers = modifiers == null ? Set.of() : modifiers;
        this.exceptionHandler =
                exceptionHandler != null ? exceptionHandler : new LoggingExceptionHandler<>();
    }

    public HandlerList(List<? extends Handler<I>> handlers) {
        this(handlers, Set.of(), new LoggingExceptionHandler<>());
    }

    @Override
    protected Handled handleInternal(I input) {
        List<Exception> errors = new ArrayList<>();
        boolean failed = false;
        boolean done = false;
        for (Handler<I> handler : handlers) {
            Handled handled;
            try {
                handled = handle(handler, input);
            } catch (Exception e) {
                handled = exceptionHandler.handle(new Failed<>(input, e));
            }
            if (handled.isDone()) {
                done = true;
                if (!should(CONTINUE_ON_DONE)) {
                    return handled;
                }
            }
            if (handled.isError()) {
                failed = true;
                errors.add(handled.exception());
                if (!should(CONTINUE_ON_ERROR)) {
                    return error(combine(errors));
                }
            }
        }
        if (failed) {
            return error(combine(errors));
        }
        if (done) {
            return ok();
        }
        return skipped();
    }

    private static Exception combine(List<Exception> errors) {
        if (errors.size() == 1) {
            return errors.get(0);
        }
        return new HandlerException(errors);
    }

    private boolean should(HandlerModifier modifier) {
        return modifiers.contains(modifier);
    }

    protected Handled handle(Handler<I> handler, I input) {
        return handler.handle(input);
    }
}
