package ru.operation.processor.impl;

import static ru.operation.processor.Processed.of;
import static ru.operation.processor.Processed.skipped;
import static ru.operation.util.Checks.notNull;

import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.util.function.ThrowableFunction;

/**
 * Runs a function and lets exceptions escape, so an outer catching wrapper can map them. A null
 * result is {@link Processed#skipped()}. Unlike {@link SimpleProcessor}, this does not turn
 * failures into {@link Processed#error(Exception)} itself.
 */
public class PropagatingProcessor<I, O> implements Processor<I, O> {

    private final ThrowableFunction<I, O> transform;

    public PropagatingProcessor(ThrowableFunction<I, O> transform) {
        this.transform = notNull(transform, "transform");
    }

    @Override
    public Processed<O> process(I input) {
        try {
            O transformed = transform.apply(input);
            return transformed == null ? skipped() : of(transformed);
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
