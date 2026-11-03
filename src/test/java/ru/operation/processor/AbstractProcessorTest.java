package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class AbstractProcessorTest {

    @Test
    public void failedCheckSkipsInternal() {
        AtomicBoolean called = new AtomicBoolean();
        Processor<String, String> processor =
                new AbstractProcessor<>() {
                    @Override
                    protected boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Processed<String> processInternal(String input) {
                        called.set(true);
                        return done(input);
                    }
                };

        assertTrue(processor.process("no").isSkipped());
        assertFalse(called.get());

        assertEquals("ok", processor.process("ok").value());
        assertTrue(called.get());
    }

    @Test
    public void checkExceptionBecomesErrorAndSkipsInternal() {
        IllegalStateException boom = new IllegalStateException("boom");
        AtomicBoolean called = new AtomicBoolean();
        Processor<String, String> processor =
                new AbstractProcessor<>() {
                    @Override
                    protected boolean check(String input) {
                        throw boom;
                    }

                    @Override
                    protected Processed<String> processInternal(String input) {
                        called.set(true);
                        return done(input);
                    }
                };

        Processed<String> result = processor.process("x");
        assertTrue(result.isError());
        assertSame(boom, result.exception());
        assertFalse(called.get());
    }
}
