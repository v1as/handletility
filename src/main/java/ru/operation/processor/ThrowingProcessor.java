package ru.operation.processor;

import static lombok.AccessLevel.PACKAGE;
import static ru.operation.processor.Processed.done;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.util.function.ThrowableFunction;

/**
 * Runs a function and lets exceptions escape, so an outer catching wrapper can map them. A {@code
 * null} result is {@link Processed#done(Object) DONE with a null payload}. Unlike {@link
 * FunctionProcessor}, this does not turn failures into {@link Processed#error(Exception)} itself.
 */
@RequiredArgsConstructor(access = PACKAGE)
final class ThrowingProcessor<I, O> implements Processor<I, O> {

    @NonNull private final ThrowableFunction<I, O> transform;

    @Override
    public Processed<O> process(I input) {
        try {
            return done(transform.apply(input));
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
        return "Throwing(" + transform + ")";
    }
}
