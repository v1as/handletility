package ru.operation.util;

import java.util.Collection;
import java.util.List;

/** Collection helpers. */
public final class Collections {

    private Collections() {}

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <E, C extends Collection<E>> C requireNonEmpty(C collection, String name) {
        if (isNullOrEmpty(collection)) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return collection;
    }

    public static <E> List<E> copyNonEmpty(List<E> list, String name) {
        return List.copyOf(requireNonEmpty(list, name));
    }
}
