package ru.operation.handler.impl.switching;

import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;
import ru.operation.identifier.IdentifyingProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

/**
 * Ключ из входа читается один раз через {@code keyReader} (равенство ключей); выбирается ровно один
 * {@link IdentifiedHandler} — его done, skipped и error становятся окончательным результатом. Skip
 * {@code keyReader} и неизвестный ключ направляются в {@code defaultHandler}, иначе — skipped.
 * Error от {@code keyReader} возвращается как есть, {@code defaultHandler} не вызывается.
 */
public class SwitchHandler<K, I> extends AbstractHandler<I> {

    private final IdentifyingProcessor<K, ? extends IdentifiedHandler<K, I>, I> lookup;
    private final Handler<I> defaultHandler;

    @Builder(builderMethodName = "switchHandler")
    public SwitchHandler(
            @NonNull @Singular List<? extends IdentifiedHandler<K, I>> handlers,
            @NonNull Processor<I, K> keyReader,
            Handler<I> defaultHandler) {
        this.lookup = new IdentifyingProcessor<>(handlers, keyReader);
        this.defaultHandler = defaultHandler;
    }

    @Override
    protected Handled handleInternal(I input) {
        log.trace("Handling input '{}'", input);
        Processed<? extends IdentifiedHandler<K, I>> handler = lookup.process(input);
        if (handler.isError()) {
            log.trace("Key read failed", handler.exception());
            return error(handler.exception());
        }
        if (handler.hasValue()) {
            IdentifiedHandler<K, I> selected = handler.value();
            Handled handled = selected.handle(input);
            log.trace(
                    "Handler '{}' ({}) has result '{}'",
                    selected.getClass().getSimpleName(),
                    selected.identifier(),
                    handled);
            return handled;
        }
        if (defaultHandler != null) {
            Handled handled = defaultHandler.handle(input);
            log.trace(
                    "Default handler '{}' has result '{}'",
                    defaultHandler.getClass().getSimpleName(),
                    handled);
            return handled;
        }
        log.trace("No handler matched, result '[SKIPPED]'");
        return skipped();
    }
}
