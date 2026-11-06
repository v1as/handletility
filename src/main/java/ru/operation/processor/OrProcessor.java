package ru.operation.processor;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.operation.util.Checks.notNull;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Tries processors in order on the same input until one returns {@link Processed#done(Object)} or
 * {@link Processed#error(Exception)}; otherwise returns {@link Processed#skipped()} when every step
 * skipped. Built via {@link Processor#or}; nested {@code or} chains flatten into one list.
 * Exceptions thrown by a delegate propagate to the caller (no mapping to {@link
 * Processed#error(Exception)}).
 */
final class OrProcessor<I, O> implements Processor<I, O> {

    private final Logger log = getLogger(getClass());
    private final List<Processor<? super I, O>> steps;

    OrProcessor(Processor<? super I, O> left, Processor<? super I, O> right) {
        this.steps = new ArrayList<>();
        if (left instanceof OrProcessor) {
            @SuppressWarnings("unchecked")
            OrProcessor<I, O> orLeft = (OrProcessor<I, O>) left;
            this.steps.addAll(orLeft.steps);
        } else {
            this.steps.add(left);
        }
        this.steps.add(notNull(right, "next"));
    }

    @Override
    public Processed<O> process(I input) {
        log.trace("Handling input '{}'", input);
        for (Processor<? super I, O> processor : steps) {
            Processed<O> processed = processor.process(input);
            log.trace(
                    "Processor '{}' has result '{}'",
                    processor.getClass().getSimpleName(),
                    processed);
            if (processed.isError() || processed.isDone()) {
                log.trace("Processed '{}' to '{}'", input, processed);
                return processed;
            }
        }
        Processed<O> skipped = Processed.skipped();
        log.trace("Processed '{}' to '{}'", input, skipped);
        return skipped;
    }

    @Override
    public String toString() {
        return "Or(" + steps + ")";
    }
}
