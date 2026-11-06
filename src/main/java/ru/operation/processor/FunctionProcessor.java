package ru.operation.processor;

import static lombok.AccessLevel.PACKAGE;
import static ru.operation.processor.Processed.done;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.util.function.ThrowableFunction;

/** Applies a {@link ThrowableFunction}; {@code null} result → {@link Processed#done(Object)}. */
@RequiredArgsConstructor(access = PACKAGE)
class FunctionProcessor<I, O> extends AbstractProcessor<I, O> {

    @NonNull private final ThrowableFunction<I, O> transform;

    @Override
    protected Processed<O> processInternal(I input) throws Exception {
        return done(transform.apply(input));
    }
}
