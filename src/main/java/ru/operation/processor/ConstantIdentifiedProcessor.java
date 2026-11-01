package ru.operation.processor;

import lombok.NonNull;

record ConstantIdentifiedProcessor<K, I, O>(
        @NonNull K identifier, @NonNull Processor<I, O> delegate)
        implements IdentifiedProcessor<K, I, O> {

    @Override
    public Processed<O> process(I input) {
        return delegate.process(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
