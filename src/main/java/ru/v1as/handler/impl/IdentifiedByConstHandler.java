package ru.v1as.handler.impl;

import lombok.RequiredArgsConstructor;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.IdentifiedHandler;

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
