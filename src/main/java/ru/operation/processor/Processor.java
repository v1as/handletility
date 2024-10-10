package ru.operation.processor;

@FunctionalInterface
public interface Processor<I, O> {

    Processed<O> process(I input);
}
