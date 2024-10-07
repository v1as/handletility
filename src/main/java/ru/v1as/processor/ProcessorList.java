package ru.v1as.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.v1as.Modifier.STOP_ON_ERROR;
import static ru.v1as.ResultState.DONE;
import static ru.v1as.ResultState.ERROR;
import static ru.v1as.processor.Processed.error;
import static ru.v1as.processor.Processed.skipped;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import ru.v1as.Modifier;
import ru.v1as.processor.impl.LoggingExceptionProcessor;

@RequiredArgsConstructor
public class ProcessorList<I, O> implements Processor<I, O> {

    private final Logger log = getLogger(this.getClass());
    private final String name;
    private final List<Processor<I, O>> processors;
    private final Set<Modifier> modifiers;
    private final Processor<Exception, O> exceptionProcessor;

    public ProcessorList(String name, List<Processor<I, O>> processors) {
        this(name, processors, Set.of(), new LoggingExceptionProcessor<>());
    }

    @Override
    public Processed<O> process(I input) {
        ProcessorException exception = null;
        for (Processor<I, O> processor : processors) {
            Processed<O> processed;
            try {
                processed = processor.process(input);
            } catch (Exception e) {
                log.warn("Processing '{}' error", name, e);
                processed = exceptionProcessor.process(e);
            }
            if (log.isTraceEnabled()) {
                log.trace(
                        "Processor '{}' has result '{}'",
                        processor.getClass().getSimpleName(),
                        processed);
            }
            if (DONE.equals(processed.state())) {
                logResult(processed);
                return processed;
            }
            if (ERROR.equals(processed.state())) {
                if (exception == null) {
                    exception = new ProcessorException("Processing '%s' error".formatted(name));
                }
                exception.addSuppressed(processed.exception());
                if (should(STOP_ON_ERROR)) {
                    return error(exception);
                }
            }
        }
        if (exception != null) {
            Processed<O> result = Processed.error(exception);
            logResult(result);
            return result;
        }
        logResult(skipped());
        return skipped();
    }

    private boolean should(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    private void logResult(Processed<O> result) {
        if (log.isDebugEnabled()) {
            log.debug("Processing '{}' result: {}", name, result);
        }
    }
}
