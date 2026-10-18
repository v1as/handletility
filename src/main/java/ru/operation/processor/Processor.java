package ru.operation.processor;

/**
 * Transforms {@code I} into {@code O}. Prefer extending {@link AbstractProcessor} for check,
 * exception handling and logging.
 */
@FunctionalInterface
public interface Processor<I, O> {

    Processed<O> process(I input);
}
