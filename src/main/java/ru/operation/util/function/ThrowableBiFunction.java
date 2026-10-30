package ru.operation.util.function;

@FunctionalInterface
public interface ThrowableBiFunction<T, U, R> {
    R apply(T t, U u) throws Exception;
}
