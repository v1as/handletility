package ru.v1as.handler.impl.map;

import static ru.v1as.handler.Handled.error;

import lombok.Builder;
import lombok.Singular;

import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.IdentifiedHandler;
import ru.v1as.identifier.IdentifyProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

import java.util.List;

public class HandlerMap<K, I> extends AbstractHandler<I> {

    private final IdentifyProcessor<K, ? extends IdentifiedHandler<K, I>, I> handlerIdentifier;
    private final Handler<I> defaultHandler;

    @Builder(builderMethodName = "handlerMap")
    public HandlerMap(
            @Singular List<? extends IdentifiedHandler<K, I>> handlers,
            Processor<I, K> keyExtractor,
            Handler<I> defaultHandler) {
        this.handlerIdentifier = new IdentifyProcessor<>(handlers, keyExtractor);
        this.defaultHandler = defaultHandler;
    }

    @Override
    protected Handled handleInternal(I input) {
        Processed<? extends IdentifiedHandler<K, I>> handler = handlerIdentifier.process(input);
        if (!handler.isEmpty()) {
            return handler.value().handle(input);
        }
        if (defaultHandler != null) {
            return defaultHandler.handle(input);
        }
        String reason = handler.isError() ? " " + handler : "";
        return error("No handler identified" + reason);
    }
}
