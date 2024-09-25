package ru.v1as.processor;

import static ru.v1as.processor.Processed.skipped;

public abstract class AbstractProcessor<I, O> implements Processor<I, O> {

    @Override
    public Processed<O> process(I input) {
        try {
            if (!check(input)) {
                return skipped();
            }
            final O result = transform(input);
            return result == null ? skipped() : Processed.processed(result);
        } catch (Exception e) {
            return Processed.error(e);
        }
    }

    protected boolean check(I input) {
        return true;
    }

    protected abstract O transform(I input);
}
