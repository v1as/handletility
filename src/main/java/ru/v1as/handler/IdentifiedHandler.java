package ru.v1as.handler;

import ru.v1as.identifier.Identified;

public interface IdentifiedHandler<K, T> extends Handler<T>, Identified<K> {}
