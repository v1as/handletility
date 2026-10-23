package ru.operation.handler;

import ru.operation.ResultState;
import ru.operation.processor.Processed;
import ru.operation.util.function.ThrowableSupplier;

/** Result of a {@link Handler}: done, skipped, or error (with optional exception). */
public record Handled(ResultState state, Exception exception) {

    private static final Handled OK = new Handled(ResultState.DONE, null);
    public static final Handled SKIPPED = new Handled(ResultState.SKIPPED, null);

    public static Handled error(String reason) {
        return error(new HandlerException(reason));
    }

    public static Handled error(Exception exception) {
        return new Handled(ResultState.ERROR, exception);
    }

    public static Handled ok() {
        return OK;
    }

    public static Handled skipped() {
        return SKIPPED;
    }

    public boolean isError() {
        return state == ResultState.ERROR;
    }

    public boolean isSkipped() {
        return state == ResultState.SKIPPED;
    }

    public boolean isHandled() {
        return state == ResultState.DONE;
    }

    public Handled orElseThrow() {
        if (isError()) {
            throw new HandlerException("Handling error", exception);
        }
        return this;
    }

    /**
     * Continues with a value when this outcome is handled. Skip and error propagate. {@code null}
     * from {@code next} is {@link Processed#skipped()}; an exception from {@code next} is {@link
     * Processed#error(Exception)}.
     */
    public <O> Processed<O> thenGet(ThrowableSupplier<O> next) {
        if (isError()) {
            return Processed.error(exception);
        }
        if (isSkipped()) {
            return Processed.skipped();
        }
        try {
            O value = next.get();
            return value == null ? Processed.skipped() : Processed.of(value);
        } catch (Exception e) {
            return Processed.error(e);
        }
    }

    @Override
    public String toString() {
        String result = state.toString();
        if (exception != null) {
            result += ": %s %s".formatted(exception.getClass(), exception.getMessage());
        }
        return "[" + result + "]";
    }
}
