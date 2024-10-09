package ru.v1as.processor;

import static ru.v1as.ResultState.DONE;
import static ru.v1as.ResultState.ERROR;

import java.util.Optional;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import ru.v1as.ResultState;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class Processed<T> {

    private static final Processed<?> SKIPPED = new Processed<>(null, ResultState.SKIPPED, null);
    private final T value;
    private final ResultState state;
    private final Exception exception;

    private Processed(T value, ResultState state, Exception exception) {
        this.value = value;
        this.state = state;
        this.exception = exception;
    }

    public static <O> Processed<O> error(String reason) {
        return error(new ProcessorException(reason));
    }

    public static <O> Processed<O> error(Exception exception) {
        return new Processed<>(null, ResultState.ERROR, exception);
    }

    public static <O> Processed<O> processed(O result) {
        return new Processed<>(result, DONE, null);
    }

    @SuppressWarnings("unchecked")
    public static <O> Processed<O> skipped() {
        return (Processed<O>) SKIPPED;
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(value);
    }

    @SuppressWarnings("unchecked")
    public <O> Processed<O> map(Function<T, O> transform) {
        if (isEmpty()) {
            return (Processed<O>) this;
        }
        return new Processed<>(transform.apply(value), state, exception);
    }

    public boolean isEmpty() {
        return value == null;
    }

    @SuppressWarnings("unchecked")
    public <O> Processed<O> process(Processor<T, O> processor) {
        if (isError()) {
            return (Processed<O>) this;
        }
        if (isEmpty()) {
            return skipped();
        }
        return processor.process(value);
    }

    public Handled handle(Handler<T> handler) {
        if (isError()) {
            return Handled.error(exception);
        }
        if (isEmpty()) {
            return Handled.skipped();
        }
        return handler.handle(value);
    }

    public boolean isError() {
        return exception != null;
    }

    public T orThrow() {
        if (value == null && exception != null) {
            throw new ProcessorException(exception);
        }
        return value;
    }

    public T value() {
        return value;
    }

    public ResultState state() {
        return state;
    }

    public Exception exception() {
        return exception;
    }

    @Override
    public String toString() {
        String result = state.toString();
        if (DONE == state) {
            result += value != null ? String.valueOf(value) : "";
        } else if (ERROR == state) {
            result +=
                    exception != null
                            ? exception.getClass().getSimpleName() + ":" + exception.getMessage()
                            : "";
        }
        return result;
    }

    public <O> Processed<O> accumulate(T processor) {
        return null;
    }
}
