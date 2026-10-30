package ru.operation.handler;

import ru.operation.identifier.Identified;

public interface IdentifiedHandler<K, I> extends Handler<I>, Identified<K> {}
