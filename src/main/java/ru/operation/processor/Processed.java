package ru.operation.processor;

import static ru.operation.ResultState.DONE;
import static ru.operation.ResultState.ERROR;
import static ru.operation.ResultState.SKIPPED;

import java.util.Optional;
import java.util.function.Function;
import ru.operation.ResultState;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/**
 * Result of a {@link Processor}. {@link ResultState#DONE} may carry a {@code null} payload; {@link
 * #hasValue()} is {@code true} only for a non-null payload. Skip and error never carry a value.
 * Done and skip never carry an exception; error always does.
 */
public record Processed<T>(T value, ResultState state, Exception exception) {

    public Processed {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        if (state == DONE) {
            if (exception != null) {
                throw new IllegalArgumentException("DONE cannot carry an exception");
            }
        } else if (state == SKIPPED) {
            if (value != null || exception != null) {
                throw new IllegalArgumentException("SKIPPED cannot carry a value or an exception");
            }
        } else if (value != null || exception == null) {
            throw new IllegalArgumentException("ERROR requires an exception and no value");
        }
    }

    private static final Processed<?> SKIPPED_RESULT =
            new Processed<>(null, ResultState.SKIPPED, null);

    public static <O> Processed<O> error(String reason) {
        return error(new ProcessorException(reason));
    }

    public static <O> Processed<O> error(Exception exception) {
        return new Processed<>(null, ResultState.ERROR, exception);
    }

    public static <O> Processed<O> of(O result) {
        return new Processed<>(result, DONE, null);
    }

    @SuppressWarnings("unchecked")
    public static <O> Processed<O> skipped() {
        return (Processed<O>) SKIPPED_RESULT;
    }

    public Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    @SuppressWarnings("unchecked")
    public <O> Processed<O> map(Function<T, O> transform) {
        if (isError() || isSkipped()) {
            return (Processed<O>) this;
        }
        return new Processed<>(transform.apply(value), state, exception);
    }

    /** {@code true} when the payload is non-null ({@link ResultState#DONE} may still hold {@code null}). */
    public boolean hasValue() {
        return value != null;
    }

    @SuppressWarnings("unchecked")
    public <O> Processed<O> flatMap(Processor<T, O> processor) {
        if (isError()) {
            return (Processed<O>) this;
        }
        if (isSkipped()) {
            return skipped();
        }
        return processor.process(value);
    }

    public Handled thenHandle(Handler<T> handler) {
        if (isError()) {
            return Handled.error(exception);
        }
        if (isSkipped()) {
            return Handled.skipped();
        }
        return handler.handle(value);
    }

    public boolean isError() {
        return state == ERROR;
    }

    public boolean isSkipped() {
        return state == SKIPPED;
    }

    public boolean isDone() {
        return state == DONE;
    }

    public T throwIfError() {
        if (!isError()) {
            return value;
        }
        if (exception instanceof RuntimeException runtime) {
            throw runtime;
        }
        throw new ProcessorException(exception.getMessage(), exception);
    }

    @Override
    public String toString() {
        String result = "[" + state.toString();
        if (DONE == state) {
            result += value != null ? ": " + value : ":";
        } else if (ERROR == state) {
            result += ": " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
        return result + "]";
    }
}
