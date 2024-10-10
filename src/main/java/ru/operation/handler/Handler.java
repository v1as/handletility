package ru.operation.handler;

@FunctionalInterface
public interface Handler<I> {

    Handled handle(I input);
}
