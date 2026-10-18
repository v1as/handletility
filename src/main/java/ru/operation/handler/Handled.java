package ru.operation.handler;

import ru.operation.ResultState;

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

    @Override
    public String toString() {
        String result = state.toString();
        if (exception != null) {
            result += ": %s %s".formatted(exception.getClass(), exception.getMessage());
        }
        return "[" + result + "]";
    }
}
