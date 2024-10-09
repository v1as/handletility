package ru.v1as.handler;

@FunctionalInterface
public interface Handler<I> {

    Handled handle(I input);
}
