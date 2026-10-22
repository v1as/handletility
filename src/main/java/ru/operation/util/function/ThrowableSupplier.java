package ru.operation.util.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowableSupplier<T> {
    static <T> Supplier<T> wrapExceptions(ThrowableSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    T get() throws Exception;
}
