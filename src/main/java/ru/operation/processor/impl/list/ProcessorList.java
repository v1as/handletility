package ru.operation.processor.impl.list;

import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;
import static ru.operation.processor.impl.list.ProcessorModifier.CONTINUE_ON_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import ru.operation.Failed;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
import ru.operation.processor.impl.LoggingExceptionProcessor;

/**
 * Tries processors in order until one returns {@code DONE}. Stops on {@code ERROR} unless {@link
 * ProcessorModifier#CONTINUE_ON_ERROR} is set.
 */
@RequiredArgsConstructor
public class ProcessorList<I, O> extends AbstractProcessor<I, O> {

    private final List<? extends Processor<I, O>> processors;
    private final Set<ProcessorModifier> modifiers;
    private final Processor<Failed<I>, O> exceptionProcessor;

    public ProcessorList(List<? extends Processor<I, O>> processors) {
        this(processors, Set.of(), new LoggingExceptionProcessor<>());
    }

    @Override
    protected Processed<O> processInternal(I input) throws Exception {
        List<Exception> errors = new ArrayList<>();
        boolean failed = false;
        Set<ProcessorModifier> effectiveModifiers = modifiers == null ? Set.of() : modifiers;
        Processor<Failed<I>, O> onException =
                exceptionProcessor != null ? exceptionProcessor : new LoggingExceptionProcessor<>();
        for (Processor<I, O> processor : processors) {
            Processed<O> processed;
            try {
                processed = processor.process(input);
            } catch (Exception e) {
                log.warn("Processing error", e);
                processed = onException.process(new Failed<>(input, e));
            }
            log.trace(
                    "Processor '{}' has result '{}'",
                    processor.getClass().getSimpleName(),
                    processed);
            if (processed.isDone()) {
                return processed;
            }
            if (processed.isError()) {
                failed = true;
                errors.add(processed.exception());
                if (!effectiveModifiers.contains(CONTINUE_ON_ERROR)) {
                    return error(combine(errors));
                }
            }
        }
        if (failed) {
            return error(combine(errors));
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
