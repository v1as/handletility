package ru.v1as.handler.impl.list;

import static ru.v1as.ResultState.DONE;
import static ru.v1as.ResultState.ERROR;
import static ru.v1as.handler.Handled.error;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;
import static ru.v1as.handler.impl.list.HandlerModifier.CONTINUE_ON_DONE;
import static ru.v1as.handler.impl.list.HandlerModifier.STOP_ON_ERROR;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import ru.v1as.Failed;
import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.HandlerException;
import ru.v1as.handler.impl.LoggingExceptionHandler;

@RequiredArgsConstructor
public class HandlerList<I> extends AbstractHandler<I> {

    private final List<? extends Handler<I>> handlers;
    private final Set<HandlerModifier> modifiers;
    private final Handler<Failed<I>> exceptionHandler;

    public HandlerList(List<? extends Handler<I>> handlers) {
        this(handlers, Set.of(), new LoggingExceptionHandler<>());
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
                if (should(STOP_ON_ERROR)) {
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
