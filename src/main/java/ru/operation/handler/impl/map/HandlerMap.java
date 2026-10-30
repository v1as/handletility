package ru.operation.handler.impl.map;

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
import ru.operation.identifier.IdentifyProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Routes input to an {@link IdentifiedHandler} selected by a key extracted from the input. A
 * key-extraction error is returned as-is and does not call {@code defaultHandler}. Skip and an
 * unknown key do.
 */
public class HandlerMap<K, I> extends AbstractHandler<I> {

    private final IdentifyProcessor<K, ? extends IdentifiedHandler<K, I>, I> handlerIdentifier;
    private final Handler<I> defaultHandler;

    @Builder(builderMethodName = "handlerMap")
    public HandlerMap(
            @NonNull @Singular List<? extends IdentifiedHandler<K, I>> handlers,
            @NonNull Processor<I, K> keyExtractor,
            Handler<I> defaultHandler) {
        this.handlerIdentifier = new IdentifyProcessor<>(handlers, keyExtractor);
        this.defaultHandler = defaultHandler;
    }

    @Override
    protected Handled handleInternal(I input) {
        Processed<? extends IdentifiedHandler<K, I>> handler = handlerIdentifier.process(input);
        if (handler.hasValue()) {
            log.trace("Using handler {}", handler);
            return handler.value().handle(input);
        }
        if (handler.isError()) {
            return error(handler.exception());
        }
        if (defaultHandler != null) {
            log.trace("Using default handler");
            return defaultHandler.handle(input);
        }
        return skipped();
    }
}
