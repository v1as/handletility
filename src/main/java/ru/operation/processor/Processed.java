package ru.operation.processor;

import static ru.operation.ResultState.DONE;
import static ru.operation.ResultState.ERROR;

import java.util.Optional;
import java.util.function.Function;
import ru.operation.ResultState;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public record Processed<T>(T value, ResultState state, Exception exception) {

    private static final Processed<?> SKIPPED = new Processed<>(null, ResultState.SKIPPED, null);

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

    @Override
    public String toString() {
        String result = "[" + state.toString();
        if (DONE == state) {
            result += value != null ? ": " + value : ":";
        } else if (ERROR == state) {
            result +=
                    exception != null
                            ? ": "
                                    + exception.getClass().getSimpleName()
                                    + ": "
                                    + exception.getMessage()
                            : "";
        }
        return result + "]";
    }
}
