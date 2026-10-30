package ru.operation.identifier;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/** Resolves an {@link Identified} item by extracting a key from the input. */
public class IdentifyProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    @Getter private final List<T> items;
    private final Map<K, T> keyToItem;
    private final Processor<I, K> keyProcessor;

    public IdentifyProcessor(@NonNull List<T> items, @NonNull Processor<I, K> keyProcessor) {
        this.keyToItem =
                items.stream().collect(Collectors.toMap(Identified::getIdentifier, identity()));
        this.keyProcessor = keyProcessor;
        this.items = items;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = keyProcessor.process(input);
        Processed<T> identified = key.map(keyToItem::get);
        if (identified.hasValue()) {
            log.trace("For '{}' identified by key '{}'", input, key);
        } else {
            log.trace("For '{}' not identified by key {}", input, key);
        }
        return identified;
    }
}
