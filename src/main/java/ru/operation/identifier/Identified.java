package ru.operation.identifier;

/** Marker for entities selectable by a stable identifier. */
public interface Identified<K> {

    K identifier();
}
