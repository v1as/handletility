package ru.operation.processor.impl.list;

import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;
import static ru.operation.processor.impl.list.ProcessorModifier.CONTINUE_ON_ERROR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import ru.operation.Failure;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
import ru.operation.processor.impl.LoggingExceptionProcessor;
import ru.operation.util.function.ThrowableBiFunction;

/**
 * Runs processors over one input. Without {@code combiner}, the first {@code DONE} is returned.
 * With {@code combiner}, every {@code DONE} is folded left to right. {@code SKIPPED} moves on. Stops
 * on {@code ERROR} unless {@link ProcessorModifier#CONTINUE_ON_ERROR} is set. An exception from
 * {@code combiner} is an error of the list and follows the same rule. A collected error during a
 * fold is still the result, and the accumulated value is dropped.
 */
public class ProcessorList<I, O> extends AbstractProcessor<I, O> {

    private final List<? extends Processor<I, O>> processors;
    private final Set<ProcessorModifier> modifiers;
    private final ThrowableBiFunction<O, O, O> combiner;
    private final Processor<Failure<I>, O> onException;

    @Builder(builderMethodName = "processorList")
    public ProcessorList(
            @NonNull @Singular List<? extends Processor<I, O>> processors,
            @Singular Set<ProcessorModifier> modifiers,
            ThrowableBiFunction<O, O, O> combiner,
            Processor<Failure<I>, O> onException) {
        this.processors = processors;
        this.modifiers = modifiers == null ? Collections.<ProcessorModifier>emptySet() : modifiers;
        this.combiner = combiner;
        this.onException = onException != null ? onException : new LoggingExceptionProcessor<>();
    }

    public ProcessorList(List<? extends Processor<I, O>> processors) {
        this(
                processors,
                Collections.<ProcessorModifier>emptySet(),
                null,
                new LoggingExceptionProcessor<>());
    }

    @Override
    protected Processed<O> processInternal(I input) {
        List<Exception> errors = new ArrayList<>();
        boolean failed = false;
        boolean hasDone = false;
        O accumulator = null;
        for (Processor<I, O> processor : processors) {
            Processed<O> processed;
            try {
                processed = processor.process(input);
            } catch (Exception e) {
                log.trace(
                        "Processor '{}' threw while processing '{}'",
                        processor.getClass().getSimpleName(),
                        input,
                        e);
                processed = this.onException.process(new Failure<>(input, e));
            }
            log.trace(
                    "Processor '{}' has result '{}'",
                    processor.getClass().getSimpleName(),
                    processed);
            if (processed.isError()) {
                failed = true;
                errors.add(processed.exception());
                if (!modifiers.contains(CONTINUE_ON_ERROR)) {
                    return error(combine(errors));
                }
                continue;
            }
            if (processed.isDone() && combiner != null) {
                if (!hasDone) {
                    accumulator = processed.value();
                    hasDone = true;
                    continue;
                }
                try {
                    accumulator = combiner.apply(accumulator, processed.value());
                    continue;
                } catch (Exception exception) {
                    log.trace("Combiner threw while processing '{}'", input, exception);
                    Processed<O> mapped = onException.process(new Failure<>(input, exception));
                    failed = true;
                    errors.add(mapped.isError() ? mapped.exception() : exception);
                    if (!modifiers.contains(CONTINUE_ON_ERROR)) {
                        return error(combine(errors));
                    }
                    continue;
                }
            }
            if (processed.isDone()) {
                return processed;
            }
        }
        if (failed) {
            return error(combine(errors));
        }
        if (hasDone) {
            return done(accumulator);
        }
        return skipped();
    }

    private static Exception combine(List<Exception> errors) {
        if (errors.size() == 1) {
            return errors.get(0);
        }
        return new ProcessorException(errors);
    }
}
