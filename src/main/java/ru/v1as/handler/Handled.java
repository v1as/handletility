package ru.v1as.handler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import ru.v1as.ResultState;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class Handled {

    private static final Handled HANDLED = new Handled(null, ResultState.DONE);
    public static final Handled SKIPPED = new Handled(null, ResultState.SKIPPED);

    private final ResultState state;
    private final Exception exception;

    private Handled(Exception exception, ResultState state) {
        this.exception = exception;
        this.state = state;
    }

    public static Handled error(String reason) {
        return error(new HandlerException(reason));
    }

    public static Handled error(Exception exception) {
        return new Handled(exception, ResultState.ERROR);
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
