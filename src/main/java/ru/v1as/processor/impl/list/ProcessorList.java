package ru.v1as.processor.impl.list;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.ResultState.DONE;
import static ru.v1as.ResultState.ERROR;
import static ru.v1as.processor.Processed.error;
import static ru.v1as.processor.Processed.skipped;
import static ru.v1as.processor.impl.list.ProcessorModifier.STOP_ON_ERROR;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.v1as.processor.AbstractProcessor;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;
import ru.v1as.processor.ProcessorException;
import ru.v1as.processor.impl.LoggingExceptionProcessor;

@RequiredArgsConstructor
public class ProcessorList<I, O> extends AbstractProcessor<I, O> {

    private final Logger log = getLogger(this.getClass());
    private final List<Processor<I, O>> processors;
    private final Set<ProcessorModifier> modifiers;
    private final Processor<Exception, O> exceptionProcessor;

    public ProcessorList(List<Processor<I, O>> processors) {
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
                processed = exceptionProcessor.process(e);
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
                if (should(STOP_ON_ERROR)) {
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
