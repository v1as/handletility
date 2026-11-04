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

/** Resolves an {@link Identified} item by reading a key from the input via {@code keyReader}. */
public class IdentifyingProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    private final Map<K, T> byIdentifier;
    private final Processor<I, K> keyReader;

    public IdentifyingProcessor(@NonNull List<T> candidates, @NonNull Processor<I, K> keyReader) {
        this.byIdentifier =
                candidates.stream().collect(Collectors.toMap(Identified::identifier, identity()));
        this.keyReader = keyReader;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = keyReader.process(input);
        if (key.isError()) {
            log.trace("For '{}' reading key failed", input, key.exception());
            return error(key.exception());
        }
        if (key.isSkipped()) {
            log.trace("For '{}' reading key skipped", input);
            return skipped();
        }
        T item = byIdentifier.get(key.value());
        if (item != null) {
            log.trace("For '{}' identified by '{}'", input, key.value());
            return done(item);
        }
        log.trace("For '{}' not identified by '{}'", input, key.value());
        return done(null);
    }
}
