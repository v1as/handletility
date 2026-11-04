package ru.operation.processor.impl.switching;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import ru.operation.identifier.IdentifyingProcessor;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

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
        Processed<? extends Processor<I, O>> processor = lookup.process(input);
        if (processor.isError()) {
            return error(processor.exception());
        }
        if (processor.hasValue()) {
            Processor<I, O> selected = processor.value();
            log.trace("Using processor {}", selected);
            return selected.process(input);
        }
        if (defaultProcessor != null) {
            log.trace("Using default processor");
            return defaultProcessor.process(input);
        }
        return skipped();
    }
}
