package ru.operation.identifier;

import static java.util.function.Function.identity;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

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
        if (key.isError()) {
            log.trace("For '{}' key extraction failed", input, key.exception());
            return error(key.exception());
        }
        if (key.isSkipped()) {
            log.trace("For '{}' key extraction skipped", input);
            return skipped();
        }
        T item = keyToItem.get(key.value());
        if (item != null) {
            log.trace("For '{}' identified by key '{}'", input, key.value());
            return done(item);
        }
        log.trace("For '{}' not identified by key '{}'", input, key.value());
        return done(null);
    }
}
