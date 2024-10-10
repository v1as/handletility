package ru.v1as.handler.impl;

import static ru.v1as.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.HandlerException;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@RequiredArgsConstructor
public class MappedHandler<T, I> extends AbstractHandler<T> {

    private final Processor<T, I> mapper;
    private final Handler<I> handler;

    @Override
    protected Handled handleInternal(T input) {
        Processed<I> mapped = mapper.process(input);
        if (mapped.isEmpty()) {
            log.trace("Handling '{}' interrupted because of mapping: {}", input, mapped);
            return error(
                    new HandlerException(
                            "Handling error because of mapping: " + mapped, mapped.exception()));
        }
        return handler.handle(mapped.value());
    }
}
