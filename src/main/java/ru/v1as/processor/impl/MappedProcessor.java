package ru.v1as.processor.impl;

import lombok.RequiredArgsConstructor;
import ru.v1as.processor.AbstractProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;
import ru.v1as.processor.ProcessorException;

@RequiredArgsConstructor
public class MappedProcessor<T, I, O> extends AbstractProcessor<T, O> {

    private final Processor<T, I> mapper;
    private final Processor<I, O> processor;

    @Override
    protected Processed<O> processInternal(T input) {
        Processed<I> mapped = mapper.process(input);
        if (mapped.isEmpty()) {
            log.trace("Processing '{}' interrupted because of mapping: {}", input, mapped);
            return Processed.error(
                    new ProcessorException(
                            "Processing error because of mapping " + mapped, mapped.exception()));
        }
        return processor.process(mapped.value());
    }
}
