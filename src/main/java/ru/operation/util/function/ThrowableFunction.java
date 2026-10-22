package ru.operation.util.function;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowableFunction<T, R> {
    static <T, R> Function<T, R> wrapExceptions(ThrowableFunction<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    R apply(T t) throws Exception;
}
