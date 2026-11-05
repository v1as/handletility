package ru.operation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Collection argument checks. */
public final class CollectionChecks {

    private CollectionChecks() {}

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
        return unmodifiableCopy(requireNonEmpty(list, name));
    }

    static <E> List<E> unmodifiableCopy(List<? extends E> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
}
