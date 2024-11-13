package ru.operation.handler.impl.map;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;
import ru.operation.identifier.IdentifyProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

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
    public void setName(String name) {
        super.setName(name);
        handlerIdentifier.setName(name + "-identifier");
        if (defaultHandler instanceof AbstractHandler<?> abstractHandler) {
            abstractHandler.setName(name + "-default");
        }
    }

    @Override
    protected Handled handleInternal(I input) {
        Processed<? extends IdentifiedHandler<K, I>> handler = handlerIdentifier.process(input);
        if (!handler.isEmpty()) {
            log.debug("Using handler {}", handler);
            return handler.value().handle(input);
        }
        if (defaultHandler != null) {
            log.debug("Using default handler");
            return defaultHandler.handle(input);
        }
        if (handler.isError()) {
            return error("No handler identified: " + handler);
        }
        return skipped();
    }
}
