package ru.operation.handler.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

@RequiredArgsConstructor
public class MdcHandler<I, T> extends AbstractHandler<I> {
    private final String mdcKey;
    private final Processor<I, T> mdcValueProcessor;
    private final Handler<I> handler;

    @Override
    public void setName(String name) {
        super.setName(name);
        if (handler instanceof AbstractHandler<I> abstractHandler) {
            abstractHandler.setName(name + "-delegate");
        }
        if (mdcValueProcessor instanceof AbstractProcessor<?, ?> abstractProcessor) {
            abstractProcessor.setName(name + "-mdcValue");
        }
    }

    @Override
    protected Handled handleInternal(I input) throws Exception {
        String prevMdcValue = null;
        Processed<T> mdcValue = null;
        try {
            mdcValue = mdcValueProcessor.process(input);
            if (!mdcValue.isEmpty()) {
                prevMdcValue = MDC.get(mdcKey);
                MDC.put(mdcKey, prevMdcValue);
            }
            return handler.handle(input);
        } finally {
            if (mdcValue != null && !mdcValue.isEmpty()) {
                MDC.put(mdcKey, prevMdcValue);
            }
        }
    }
}
