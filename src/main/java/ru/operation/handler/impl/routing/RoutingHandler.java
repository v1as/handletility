package ru.operation.handler.impl.routing;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;
import ru.operation.identifier.IdentifyingProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Routes input to an {@link IdentifiedHandler} selected by an identifier extracted from the
 * input. An identifier-extraction error is returned as-is and does not call {@code defaultHandler}.
 * Skip and an unknown identifier do.
 */
public class RoutingHandler<K, I> extends AbstractHandler<I> {

    private final IdentifyingProcessor<K, ? extends IdentifiedHandler<K, I>, I> lookup;
    private final Handler<I> defaultHandler;

    @Builder(builderMethodName = "routingHandler")
    public RoutingHandler(
            @NonNull @Singular List<? extends IdentifiedHandler<K, I>> handlers,
            @NonNull Processor<I, K> identifierExtractor,
            Handler<I> defaultHandler) {
        this.lookup = new IdentifyingProcessor<>(handlers, identifierExtractor);
        this.defaultHandler = defaultHandler;
    }

    @Override
    protected Handled handleInternal(I input) {
        Processed<? extends IdentifiedHandler<K, I>> handler = lookup.process(input);
        if (handler.isError()) {
            return error(handler.exception());
        }
        if (handler.hasValue()) {
            IdentifiedHandler<K, I> selected = handler.value();
            log.trace("Using handler {}", selected);
            return selected.handle(input);
        }
        if (defaultHandler != null) {
            log.trace("Using default handler");
            return defaultHandler.handle(input);
        }
        return skipped();
    }
}
