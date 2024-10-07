package ru.v1as.handler;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.Modifier.CONTINUE_ON_DONE;
import static ru.v1as.Modifier.STOP_ON_ERROR;
import static ru.v1as.ResultState.DONE;
import static ru.v1as.ResultState.ERROR;
import static ru.v1as.handler.Handled.error;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.v1as.Modifier;
import ru.v1as.handler.impl.LoggingExceptionHandler;

@RequiredArgsConstructor
public class HandlerList<O> implements Handler<O> {

    private final Logger log = getLogger(this.getClass());

    private final String name;
    private final List<Handler<O>> handlers;
    private final Set<Modifier> modifiers;
    private final Handler<Exception> exceptionHandler;

    public HandlerList(String name, List<Handler<O>> handlers) {
        this(name, handlers, Set.of(), new LoggingExceptionHandler());
    }

    @Override
    public Handled handle(O input) {
        HandlerException exception = null;
        boolean done = false;
        for (Handler<O> handler : handlers) {
            Handled handled;
            try {
                handled = handler.handle(input);
            } catch (Exception e) {
                handled = exceptionHandler.handle(e);
            }
            if (log.isTraceEnabled()) {
                log.trace(
                        "Handling '{}' has result '{}'",
                        handler.getClass().getSimpleName(),
                        handled);
            }
            if (DONE.equals(handled.state())) {
                logResult(handled);
                done = true;
                if (!should(CONTINUE_ON_DONE)) {
                    return handled;
                }
            }
            if (ERROR.equals(handled.state())) {
                if (exception == null) {
                    exception = new HandlerException("Handling '%s' error".formatted(name));
                }
                exception.addSuppressed(handled.exception());
                if (should(STOP_ON_ERROR)) {
                    return error(exception);
                }
            }
        }
        Handled result;
        if (done) {
            result = handled();
        } else if (exception != null) {
            result = Handled.error(exception);
        } else {
            result = skipped();
        }
        logResult(result);
        return result;
    }

    private boolean should(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    private void logResult(Handled result) {
        if (log.isDebugEnabled()) {
            log.debug("Processing '{}' result: {}", name, result);
        }
    }
}
