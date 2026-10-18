package ru.operation.handler.impl;

import static ru.operation.handler.Handled.error;

import lombok.RequiredArgsConstructor;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerException;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor
public class MappedHandler<T, I> extends AbstractHandler<T> {

    private final Processor<T, I> mapper;
    private final Handler<I> delegate;

    @Override
    protected Handled handleInternal(T input) {
        Processed<I> mapped = mapper.process(input);
        if (!mapped.hasValue()) {
            log.trace("Handling '{}' interrupted because of mapping: {}", input, mapped);
            return error(
                    new HandlerException(
                            "Handling error because of mapping: " + mapped, mapped.exception()));
        }
        return delegate.handle(mapped.value());
    }
}
