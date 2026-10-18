package ru.operation.handler.impl.list;

import static ru.operation.ResultState.DONE;
import static ru.operation.ResultState.ERROR;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.handled;
import static ru.operation.handler.Handled.skipped;
import static ru.operation.handler.impl.list.HandlerModifier.CONTINUE_ON_DONE;
import static ru.operation.handler.impl.list.HandlerModifier.CONTINUE_ON_ERROR;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.Builder;
import lombok.Singular;
import ru.operation.Failed;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerException;
import ru.operation.handler.impl.LoggingExceptionHandler;
import ru.operation.handler.impl.SimpleHandler;

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
            @Singular List<? extends Handler<I>> handlers,
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

    /**
     * Builds a list that invokes {@code method} on each item (method-reference fan-out), continuing
     * after {@code DONE} by default.
     */
    public static <T, I> HandlerList<I> fromMethodRefs(List<T> items, BiConsumer<T, I> method) {
        return fromMethodRefs(
                items, method, Set.of(CONTINUE_ON_DONE), new LoggingExceptionHandler<>());
    }

    public static <T, I> HandlerList<I> fromMethodRefs(
            List<T> items,
            BiConsumer<T, I> method,
            Set<HandlerModifier> modifiers,
            Handler<Failed<I>> exceptionHandler) {
        return new HandlerList<>(
                items.stream()
                        .map(item -> new SimpleHandler<I>(c -> method.accept(item, c)))
                        .toList(),
                modifiers,
                exceptionHandler);
    }

    @Override
    protected Handled handleInternal(I input) {
        HandlerException exception = null;
        boolean done = false;
        Handled result;
        for (Handler<I> handler : handlers) {
            Handled handled;
            try {
                handled = handle(handler, input);
            } catch (Exception e) {
                handled = exceptionHandler.handle(new Failed<>(input, e));
            }
            if (DONE.equals(handled.state())) {
                done = true;
                if (!should(CONTINUE_ON_DONE)) {
                    return handled;
                }
            }
            if (ERROR.equals(handled.state())) {
                if (exception == null) {
                    exception = new HandlerException("Handling error");
                }
                exception.addSuppressed(handled.exception());
                if (!should(CONTINUE_ON_ERROR)) {
                    result = error(exception);
                    return result;
                }
            }
        }
        if (done) {
            result = handled();
        } else if (exception != null) {
            result = Handled.error(exception);
        } else {
            result = skipped();
        }
        return result;
    }

    private boolean should(HandlerModifier modifier) {
        return modifiers.contains(modifier);
    }

    protected Handled handle(Handler<I> handler, I input) {
        return handler.handle(input);
    }
}
