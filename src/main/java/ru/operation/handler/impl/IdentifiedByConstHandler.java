package ru.operation.handler.impl;

import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;

@RequiredArgsConstructor
public class IdentifiedByConstHandler<K, I> implements IdentifiedHandler<K, I> {

    private final K identifier;
    private final Handler<I> handler;

    @Override
    public Handled handle(I input) {
        return handler.handle(input);
    }

    @Override
    public K getIdentifier() {
        return identifier;
    }
}
