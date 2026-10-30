package ru.operation.processor.impl.map;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.List;
import lombok.NonNull;
import ru.operation.identifier.IdentifyProcessor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Routes input to an {@link IdentifiedProcessor} selected by a key extracted from the input. A
 * key-extraction error is returned as-is and does not call {@code defaultProcessor}. Skip and an
 * unknown key do.
 */
public class ProcessorMap<K, I, O> extends AbstractProcessor<I, O> {

    private final IdentifyProcessor<K, ? extends IdentifiedProcessor<K, I, O>, I> identifier;
    private final Processor<I, O> defaultProcessor;

    public <T extends IdentifiedProcessor<K, I, O>> ProcessorMap(
            @NonNull List<T> processors,
            @NonNull Processor<I, K> keyProcessor,
            Processor<I, O> defaultProcessor) {
        this.identifier = new IdentifyProcessor<>(processors, keyProcessor);
        this.defaultProcessor = defaultProcessor;
    }

    @Override
    protected Processed<O> processInternal(I input) {
        Processed<? extends Processor<I, O>> processor = identifier.process(input);
        if (processor.hasValue()) {
            return processor.value().process(input);
        }
        if (processor.isError()) {
            return error(processor.exception());
        }
        if (defaultProcessor != null) {
            log.trace("Using default processor");
            return defaultProcessor.process(input);
        }
        return skipped();
    }
}
