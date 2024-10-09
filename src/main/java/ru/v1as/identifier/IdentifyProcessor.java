package ru.v1as.identifier;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.v1as.processor.AbstractProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

@Slf4j
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
        if (!identified.isEmpty()) {
            log.trace("For '{}' identified by key '{}'", input, key.value());
        } else {
            log.trace("For '{}' not identified {}", input, identified);
        }
        return identified;
    }
}
