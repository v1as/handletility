package ru.operation.processor.impl;

import static ru.operation.processor.Processed.of;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.util.function.ThrowableFunction;

/**
 * Runs a function and lets exceptions escape, so an outer catching wrapper can map them. A {@code
 * null} result is {@link Processed#of(Object) DONE with a null payload}. Unlike {@link
 * SimpleProcessor}, this does not turn failures into {@link Processed#error(Exception)} itself.
 */
@RequiredArgsConstructor
public class PropagatingProcessor<I, O> implements Processor<I, O> {

    @NonNull private final ThrowableFunction<I, O> transform;

    @Override
    public Processed<O> process(I input) {
        try {
            return of(transform.apply(input));
        } catch (Exception e) {
            throw unchecked(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable exception) throws E {
        throw (E) exception;
    }

    @Override
    public String toString() {
        return "Propagating(" + transform + ")";
    }
}
