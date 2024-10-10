package ru.operation.handler;

import ru.operation.identifier.Identified;

public interface IdentifiedHandler<K, T> extends Handler<T>, Identified<K> {}
