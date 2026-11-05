package ru.operation;

import lombok.Value;
import lombok.experimental.Accessors;

/** Context of a failed step: original input plus the exception. */
@Value
@Accessors(fluent = true)
public class Failure<I> {
    I input;
    Exception exception;
}
