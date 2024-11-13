package ru.operation.identifier;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class IdentifyProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    @Getter private final List<T> items;
    private final Map<K, T> keyToItem;
    private final Processor<I, K> keyProcessor;

    public IdentifyProcessor(List<T> items, Processor<I, K> keyProcessor) {
        this.keyToItem =
                items.stream().collect(Collectors.toMap(Identified::getIdentifier, identity()));
        this.keyProcessor = keyProcessor;
        this.items = items;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = keyProcessor.process(input);
        Processed<T> identified = key.map(keyToItem::get);
        logIdentified(input, identified, key);
        return identified;
    }

    private void logIdentified(I input, Processed<T> identified, Processed<K> key) {
        if (!identified.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("For '{}' identified by key '{}'", input, key);
            } else if (nameDefined) {
                log.debug("Identified by key '{}'", key);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("For '{}' not identified by key {}", input, key);
            } else if (nameDefined) {
                log.debug("Not identified by key {}", key);
            }
        }
    }
}
