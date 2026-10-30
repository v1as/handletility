package ru.operation.handler.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;

@Getter
@RequiredArgsConstructor
public class IdentifiedByConstHandler<K, I> implements IdentifiedHandler<K, I> {

    @NonNull private final K identifier;
    @NonNull private final Handler<I> handler;

    @Override
    public Handled handle(I input) {
        return handler.handle(input);
    }

    @Override
    public K getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + handler + ")";
    }
}
