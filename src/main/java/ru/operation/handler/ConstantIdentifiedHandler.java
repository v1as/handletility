package ru.operation.handler;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
class ConstantIdentifiedHandler<K, I> implements IdentifiedHandler<K, I> {

    @NonNull K identifier;
    @NonNull Handler<I> delegate;

    @Override
    public Handled handle(I input) {
        return delegate.handle(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
