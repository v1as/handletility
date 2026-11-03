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

/** Resolves an {@link Identified} item by reading an identifier from the input. */
public class IdentifyingProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    private final Map<K, T> byIdentifier;
    private final Processor<I, K> identifier;

    public IdentifyingProcessor(@NonNull List<T> candidates, @NonNull Processor<I, K> identifier) {
        this.byIdentifier =
                candidates.stream().collect(Collectors.toMap(Identified::identifier, identity()));
        this.identifier = identifier;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> key = identifier.process(input);
        if (key.isError()) {
            log.trace("For '{}' reading identifier failed", input, key.exception());
            return error(key.exception());
        }
        if (key.isSkipped()) {
            log.trace("For '{}' reading identifier skipped", input);
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
