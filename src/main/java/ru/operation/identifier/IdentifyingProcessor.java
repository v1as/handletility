package ru.operation.identifier;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/** Resolves an {@link Identified} item by extracting a key from the input. */
public class IdentifyingProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    private final Map<K, T> keyToItem;
    private final Processor<I, K> keyExtractor;

    public IdentifyingProcessor(
            @NonNull List<T> candidates, @NonNull Processor<I, K> keyExtractor) {
        this.keyToItem =
                candidates.stream().collect(Collectors.toMap(Identified::identifier, identity()));
        this.keyExtractor = keyExtractor;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = keyExtractor.process(input);
        Processed<T> identified = key.map(keyToItem::get);
        if (identified.isError()) {
            log.trace("For '{}' key extraction failed: {}", input, identified);
        } else if (identified.hasValue()) {
            log.trace("For '{}' identified by key '{}'", input, key);
        } else {
            log.trace("For '{}' not identified by key {}", input, key);
        }
        return identified;
    }
}
