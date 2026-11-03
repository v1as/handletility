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

/** Resolves an {@link Identified} item by extracting an identifier from the input. */
public class IdentifyingProcessor<K, T extends Identified<K>, I> extends AbstractProcessor<I, T> {

    private final Map<K, T> byIdentifier;
    private final Processor<I, K> identifierExtractor;

    public IdentifyingProcessor(
            @NonNull List<T> candidates, @NonNull Processor<I, K> identifierExtractor) {
        this.byIdentifier =
                candidates.stream().collect(Collectors.toMap(Identified::identifier, identity()));
        this.identifierExtractor = identifierExtractor;
    }

    @Override
    protected Processed<T> processInternal(I input) {
        Processed<K> identifier = identifierExtractor.process(input);
        if (identifier.isError()) {
            log.trace("For '{}' identifier extraction failed", input, identifier.exception());
            return error(identifier.exception());
        }
        if (identifier.isSkipped()) {
            log.trace("For '{}' identifier extraction skipped", input);
            return skipped();
        }
        T item = byIdentifier.get(identifier.value());
        if (item != null) {
            log.trace("For '{}' identified by '{}'", input, identifier.value());
            return done(item);
        }
        log.trace("For '{}' not identified by '{}'", input, identifier.value());
        return done(null);
    }
}
