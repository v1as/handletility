package ru.v1as.handler.impl.map;

import static ru.v1as.handler.Handled.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.IdentifiedHandler;
import ru.v1as.identifier.IdentifyProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@Slf4j
public class HandlerMap<K, I> extends AbstractHandler<I> {

    private final IdentifyProcessor<K, ? extends IdentifiedHandler<K, I>, I> handlerIdentifier;
    private final Handler<I> defaultHandler;

    public <T extends IdentifiedHandler<K, I>> HandlerMap(
            List<T> handlers, Processor<I, K> keyProcessor, Handler<I> defaultHandler) {
        this.handlerIdentifier = new IdentifyProcessor<>(handlers, keyProcessor);
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
