package ru.operation.processor.impl;

import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.function.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Returns the input as {@link Processed#done(Object) done} when {@code predicate} holds, and {@link
 * Processed#skipped()} otherwise. An exception from the predicate is {@link
 * Processed#error(Exception)}. {@link Error} is not caught.
 */
@RequiredArgsConstructor
public class FilterProcessor<I> implements Processor<I, I> {

    @NonNull private final Predicate<? super I> predicate;

    @Override
    public Processed<I> process(I input) {
        try {
            return predicate.test(input) ? done(input) : skipped();
        } catch (Exception exception) {
            return error(exception);
        }
    }

    @Override
    public String toString() {
        return "Filter(" + predicate + ")";
    }
}
