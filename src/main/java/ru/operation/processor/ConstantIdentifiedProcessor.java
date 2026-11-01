package ru.operation.processor;

record ConstantIdentifiedProcessor<K, I, O>(K identifier, Processor<I, O> delegate)
        implements IdentifiedProcessor<K, I, O> {

    @Override
    public K identifier() {
        return identifier;
    }

    @Override
    public Processed<O> process(I input) {
        return delegate.process(input);
    }

    @Override
    public String toString() {
        return "Identified[" + identifier + "](" + delegate + ")";
    }
}
