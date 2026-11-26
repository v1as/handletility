package ru.operation;

import lombok.Value;
import lombok.experimental.Accessors;
import ru.operation.handler.Handled;

/** Input and {@link Handled} for a chain {@link ru.operation.handler.Handler#after} finisher. */
@Value
@Accessors(fluent = true)
public class HandlerOutcome<I> {
    I input;
    Handled handled;
}
