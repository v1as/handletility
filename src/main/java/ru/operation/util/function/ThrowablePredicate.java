package ru.operation.util.function;

import java.util.function.Predicate;

@FunctionalInterface
public interface ThrowablePredicate<T> {
    static <T> Predicate<T> wrapExceptions(ThrowablePredicate<T> predicate) {
        return t -> {
            try {
                return predicate.test(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    boolean test(T t) throws Exception;
}
