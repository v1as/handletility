package ru.operation;

import lombok.Value;
import lombok.experimental.Accessors;
import ru.operation.processor.Processed;

/** Input and {@link Processed} for a chain {@link ru.operation.processor.Processor#after} finisher. */
@Value
@Accessors(fluent = true)
public class ProcessorOutcome<I, O> {
    I input;
    Processed<O> processed;
}
