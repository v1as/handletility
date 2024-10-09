package ru.v1as.processor;

import ru.v1as.identifier.Identified;

public interface IdentifiedProcessor<K, I, O> extends Processor<I, O>, Identified<K> {}
