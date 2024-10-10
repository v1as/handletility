package ru.operation.processor;

import ru.operation.identifier.Identified;

public interface IdentifiedProcessor<K, I, O> extends Processor<I, O>, Identified<K> {}
