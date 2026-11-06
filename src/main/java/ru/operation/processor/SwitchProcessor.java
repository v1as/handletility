package ru.operation.processor;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * Ключ из входа читается один раз через {@code keyReader} (равенство ключей); выбирается ровно один
 * {@link IdentifiedProcessor} — его done, skipped и error становятся окончательным результатом.
 * Skip {@code keyReader} и неизвестный ключ направляются в {@code defaultProcessor}, иначе —
 * skipped. Error от {@code keyReader} возвращается как есть, {@code defaultProcessor} не
 * вызывается.
 */
public class SwitchProcessor<K, I, O> extends AbstractProcessor<I, O> {

    private final IdentifyingProcessor<K, ? extends IdentifiedProcessor<K, I, O>, I> lookup;
    private final Processor<I, O> defaultProcessor;

    @Builder(builderMethodName = "switchProcessor")
    public SwitchProcessor(
            @NonNull @Singular List<? extends IdentifiedProcessor<K, I, O>> processors,
            @NonNull Processor<I, K> keyReader,
            Processor<I, O> defaultProcessor) {
        this.lookup = new IdentifyingProcessor<>(processors, keyReader);
        this.defaultProcessor = defaultProcessor;
    }

    @Override
    protected Processed<O> processInternal(I input) {
        log.trace("Handling input '{}'", input);
        Processed<? extends IdentifiedProcessor<K, I, O>> processor = lookup.process(input);
        if (processor.isError()) {
            log.trace("Key read failed", processor.exception());
            return error(processor.exception());
        }
        if (processor.hasValue()) {
            IdentifiedProcessor<K, I, O> selected = processor.value();
            Processed<O> processed = selected.process(input);
            log.trace(
                    "Processor '{}' ({}) has result '{}'",
                    selected.getClass().getSimpleName(),
                    selected.identifier(),
                    processed);
            return processed;
        }
        if (defaultProcessor != null) {
            Processed<O> processed = defaultProcessor.process(input);
            log.trace(
                    "Default processor '{}' has result '{}'",
                    defaultProcessor.getClass().getSimpleName(),
                    processed);
            return processed;
        }
        log.trace("No processor matched, result '[SKIPPED]'");
        return skipped();
    }
}
