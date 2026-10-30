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
public class IdentifyingProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    @Getter private final List<T> candidates;
    private final Map<K, T> keyToItem;
    private final Processor<I, K> keyExtractor;

    public IdentifyingProcessor(
            @NonNull List<T> candidates, @NonNull Processor<I, K> keyExtractor) {
        this.keyToItem =
                candidates.stream()
                        .collect(Collectors.toMap(Identified::getIdentifier, identity()));
        this.keyExtractor = keyExtractor;
        this.candidates = candidates;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = keyExtractor.process(input);
        Processed<T> identified = key.map(keyToItem::get);
        if (identified.hasValue()) {
            log.trace("For '{}' identified by key '{}'", input, key);
        } else {
            log.trace("For '{}' not identified by key {}", input, key);
        }
        return identified;
    }
}
