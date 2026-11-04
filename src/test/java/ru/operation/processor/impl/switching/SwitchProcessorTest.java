package ru.operation.processor.impl.switching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.FunctionProcessor;

public class SwitchProcessorTest {

    public static final IdentifiedProcessor<String, Integer, Integer> ADD_PROCESSOR =
            new FunctionProcessor<Integer, Integer>(value -> ++value).identifiedBy("ADD");

    public static final Processor<Integer, Integer> ADD_TEN_PROCESSOR =
            new FunctionProcessor<>(value -> value + 10);

    public static final IdentifiedProcessor<String, Integer, Integer> SUB_PROCESSOR =
            new FunctionProcessor<Integer, Integer>(value -> --value).identifiedBy("SUB");

    public static final FunctionProcessor<Integer, String> KEY_READER =
            new FunctionProcessor<>(
                    value -> {
                        if (value > 0) {
                            return "ADD";
                        } else if (value < 0) {
                            return "SUB";
                        }
                        return "ZERO";
                    });

    @Test
    public void shouldProcessWithoutDefaultProcessor() {
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .processor(SUB_PROCESSOR)
                        .keyReader(KEY_READER)
                        .defaultProcessor(ADD_TEN_PROCESSOR)
                        .build();
        assertEquals(11, processor.process(10).value());
        assertEquals(-11, processor.process(-10).value());
        assertEquals(10, processor.process(0).value());
    }

    @Test
    public void shouldProcessWithDefaultProcessor() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(KEY_READER)
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value + 100);
                                })
                        .build();

        assertEquals(4, processor.process(3).value());
        assertFalse(defaultCalled.get());

        assertEquals(100, processor.process(0).value());
        assertTrue(defaultCalled.get());
    }

    @Test
    public void keyReaderErrorDoesNotCallDefault() {
        IllegalStateException failure = new IllegalStateException("broken parse");
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(
                                new FunctionProcessor<>(
                                        value -> {
                                            throw failure;
                                        }))
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value);
                                })
                        .build();

        Processed<Integer> result = processor.process(5);

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyReaderUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(new FunctionProcessor<>(value -> null))
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value + 10);
                                })
                        .build();

        assertEquals(15, processor.process(5).value());
        assertTrue(defaultCalled.get());
    }
}
