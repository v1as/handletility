package ru.operation.identifier;

/** Marker for entities selectable by a stable key. */
public interface Identified<K> {

    K getIdentifier();
}
