package ru.operation.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor
public class MdcProcessor<I, O, T> extends AbstractProcessor<I, O> {

    private final String mdcKey;
    private final Processor<I, T> mdcValueProcessor;
    private final Processor<I, O> processor;

    @Override
    public void setName(String name) {
        super.setName(name);
        if (processor instanceof AbstractProcessor<I, O> abstractProcessor) {
            abstractProcessor.setName(name + "-delegate");
        }
        if (mdcValueProcessor instanceof AbstractProcessor<?, ?> abstractProcessor) {
            abstractProcessor.setName(name + "-mdcValue");
        }
    }

    @Override
    protected Processed<O> processInternal(I input) {
        String prevMdcValue = null;
        Processed<T> mdcValue = null;
        try {
            mdcValue = mdcValueProcessor.process(input);
            if (!mdcValue.isEmpty()) {
                prevMdcValue = MDC.get(mdcKey);
                MDC.put(mdcKey, prevMdcValue);
            }
            return processor.process(input);
        } finally {
            if (mdcValue != null && !mdcValue.isEmpty()) {
                MDC.put(mdcKey, prevMdcValue);
            }
        }
    }
}
