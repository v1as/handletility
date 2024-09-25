package ru.v1as.handler;

@FunctionalInterface
public interface Handler<O> {

    Handled handle(O o);
}
