package ru.operation.handler;

import lombok.EqualsAndHashCode;
import ru.operation.ResultState;
import ru.operation.processor.Processed;
import ru.operation.util.function.ThrowableSupplier;

/** Result of a {@link Handler}: done, skipped, or error. Error always carries an exception. */
@EqualsAndHashCode
public final class Handled {

    private final ResultState state;
    private final Exception exception;

    public Handled(ResultState state, Exception exception) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        if (state == ResultState.ERROR) {
            if (exception == null) {
                throw new IllegalArgumentException("exception cannot be null");
            }
        } else if (exception != null) {
            throw new IllegalArgumentException(state + " cannot carry an exception");
        }
        this.state = state;
        this.exception = exception;
    }

    public ResultState state() {
        return state;
    }

    public Exception exception() {
        return exception;
    }

    private static final Handled DONE = new Handled(ResultState.DONE, null);
    private static final Handled SKIPPED = new Handled(ResultState.SKIPPED, null);

    public static Handled error(String reason) {
        return error(new HandlerException(reason));
    }

    public static Handled error(Exception exception) {
        return new Handled(ResultState.ERROR, exception);
    }

    public static Handled done() {
        return DONE;
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

    public boolean isDone() {
        return state == ResultState.DONE;
    }

    public Handled throwIfError() {
        if (!isError()) {
            return this;
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw new HandlerException(exception.getMessage(), exception);
    }

    /**
     * Requires {@link ResultState#DONE}. {@code ERROR} is thrown via {@link #throwIfError()}. {@code
     * SKIPPED} throws {@link HandlerException} without a cause.
     */
    public void requireDone() {
        throwIfError();
        if (isSkipped()) {
            throw new HandlerException("skipped");
        }
    }

    /**
     * Continues with a value when this outcome is done. Skip and error propagate. {@code null}
     * {@code null} from {@code next} is {@link Processed#done(Object) DONE with a null payload}; an
     * exception from {@code next} is {@link Processed#error(Exception)}.
     */
    public <O> Processed<O> produce(ThrowableSupplier<O> next) {
        if (isError()) {
            return Processed.error(exception);
        }
        if (isDone()) {
            try {
                return Processed.done(next.get());
            } catch (Exception e) {
                return Processed.error(e);
            }
        }
        return Processed.skipped();
    }

    @Override
    public String toString() {
        String result = state.toString();
        if (isError()) {
            result += String.format(": %s %s", exception.getClass(), exception.getMessage());
        }
        return "[" + result + "]";
    }
}
