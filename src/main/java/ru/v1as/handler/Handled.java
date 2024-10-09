package ru.v1as.handler;

import ru.v1as.ResultState;

public record Handled(ResultState state, Exception exception) {

    private static final Handled HANDLED = new Handled(ResultState.DONE, null);
    public static final Handled SKIPPED = new Handled(ResultState.SKIPPED, null);

    public static Handled error(String reason) {
        return error(new HandlerException(reason));
    }

    public static Handled error(Exception exception) {
        return new Handled(ResultState.ERROR, exception);
    }

    public static Handled handled() {
        return HANDLED;
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
}
