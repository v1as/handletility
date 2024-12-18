package ru.operation.processor.impl.list;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.ResultState.DONE;
import static ru.operation.ResultState.ERROR;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;
import static ru.operation.processor.impl.list.ProcessorModifier.CONTINUE_ON_ERROR;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.operation.Failed;
import ru.operation.processor.AbstractProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
import ru.operation.processor.impl.LoggingExceptionProcessor;

@RequiredArgsConstructor
public class ProcessorList<I, O> extends AbstractProcessor<I, O> {

    protected final Logger log = getLogger(this.getClass());

    private final List<? extends Processor<I, O>> processors;
    private final Set<ProcessorModifier> modifiers;
    private final Processor<Failed<I>, O> exceptionProcessor;

    public ProcessorList(List<? extends Processor<I, O>> processors) {
        this(processors, Set.of(), new LoggingExceptionProcessor<>());
    }

    @Override
    protected Processed<O> processInternal(I input) {
        ProcessorException exception = null;
        for (Processor<I, O> processor : processors) {
            Processed<O> processed;
            try {
                processed = processor.process(input);
            } catch (Exception e) {
                log.warn("Processing error", e);
                processed = exceptionProcessor.process(new Failed<>(input, e));
            }
            if (log.isTraceEnabled()) {
                log.trace(
                        "Processor '{}' has result '{}'",
                        processor.getClass().getSimpleName(),
                        processed);
            }
            if (DONE.equals(processed.state())) {
                return processed;
            }
            if (ERROR.equals(processed.state())) {
                if (exception == null) {
                    exception = new ProcessorException("Processing error");
                }
                exception.addSuppressed(processed.exception());
                if (!should(CONTINUE_ON_ERROR)) {
                    return error(exception);
                }
            }
        }
        if (exception != null) {
            return Processed.error(exception);
        }
        return skipped();
    }

    private boolean should(ProcessorModifier modifier) {
        return modifiers.contains(modifier);
    }
}
