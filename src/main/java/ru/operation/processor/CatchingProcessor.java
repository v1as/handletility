package ru.operation.processor;

import static lombok.AccessLevel.PACKAGE;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.Failure;

/**
 * Catches {@link Exception} from the delegate and maps it via {@code onException}. Does not catch
 * {@link Error}.
 */
@RequiredArgsConstructor(access = PACKAGE)
final class CatchingProcessor<I, O> implements Processor<I, O> {

    @NonNull private final Processor<I, O> delegate;
    @NonNull private final Processor<Failure<I>, O> onException;

    public CatchingProcessor(Processor<I, O> delegate) {
        this(delegate, new LoggingExceptionProcessor<>());
    }

    @Override
    public Processed<O> process(I input) {
        try {
            return delegate.process(input);
        } catch (Exception e) {
            return onException.process(new Failure<>(input, e));
        }
    }

    @Override
    public String toString() {
        return "Catching(" + delegate + ")";
    }
}
