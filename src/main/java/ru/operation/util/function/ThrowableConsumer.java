package ru.operation.util.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    static <T> Consumer<T> wrapExceptions(ThrowableConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    void accept(T t) throws Exception;
}
