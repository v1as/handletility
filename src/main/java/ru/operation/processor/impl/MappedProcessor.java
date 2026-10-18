package ru.operation.processor.impl;

import lombok.RequiredArgsConstructor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;

@RequiredArgsConstructor
public class MappedProcessor<T, I, O> extends AbstractProcessor<T, O> {

    private final Processor<T, I> mapper;
    private final Processor<I, O> delegate;

    @Override
    protected Processed<O> processInternal(T input) throws Exception {
        Processed<I> mapped = mapper.process(input);
        if (!mapped.hasValue()) {
            log.trace("Processing '{}' interrupted because of mapping: {}", input, mapped);
            return Processed.error(
                    new ProcessorException(
                            "Processing error because of mapping " + mapped, mapped.exception()));
        }
        return delegate.process(mapped.value());
    }
}
