package ru.operation.handler;

/**
 * Side-effect operation over an input. Prefer extending {@link AbstractHandler} for check,
 * exception handling and logging.
 */
@FunctionalInterface
public interface Handler<I> {

    Handled handle(I input);
}
