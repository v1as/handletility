package ru.v1as.processor;

@FunctionalInterface
public interface Processor<I, O> {

    Processed<O> process(I input);
}
