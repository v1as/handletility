package ru.operation.processor.impl.map;

import static ru.operation.processor.Processed.error;

import java.util.List;
import ru.operation.identifier.IdentifyProcessor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class ProcessorMap<K, I, O> extends AbstractProcessor<I, O> {

    private final IdentifyProcessor<K, ? extends IdentifiedProcessor<K, I, O>, I> identifier;
    private final Processor<I, O> defaultProcessor;

    public <T extends IdentifiedProcessor<K, I, O>> ProcessorMap(
            List<T> processors, Processor<I, K> keyProcessor, Processor<I, O> defaultProcessor) {
        this.identifier = new IdentifyProcessor<>(processors, keyProcessor);
        this.defaultProcessor = defaultProcessor;
    }

    @Override
    protected Processed<O> processInternal(I input) {
        Processed<? extends Processor<I, O>> processor = identifier.process(input);
        if (!processor.isEmpty()) {
            return processor.value().process(input);
        }
        if (defaultProcessor != null) {
            return defaultProcessor.process(input);
        }
        String reason = processor.isError() ? " " + processor : "";
        return error("No processor identified" + reason);
    }
}
