package ru.operation.util.function;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    void accept(T t) throws Exception;
}
