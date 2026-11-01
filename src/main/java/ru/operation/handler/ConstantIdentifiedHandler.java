package ru.operation.handler;

import lombok.NonNull;

record ConstantIdentifiedHandler<K, I>(@NonNull K identifier, @NonNull Handler<I> delegate)
        implements IdentifiedHandler<K, I> {

    @Override
    public Handled handle(I input) {
        return delegate.handle(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
