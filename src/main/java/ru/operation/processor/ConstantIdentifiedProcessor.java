package ru.operation.processor;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
class ConstantIdentifiedProcessor<K, I, O> implements IdentifiedProcessor<K, I, O> {

    @NonNull K identifier;
    @NonNull Processor<I, O> delegate;

    @Override
    public Processed<O> process(I input) {
        return delegate.process(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
