package ru.operation.processor.impl.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.IdentifiedByConstProcessor;
import ru.operation.processor.impl.SimpleProcessor;

public class ProcessorMapTest {

    public static final IdentifiedByConstProcessor<String, Integer, Integer> ADD_PROCESSOR =
            new IdentifiedByConstProcessor<>("ADD", new SimpleProcessor<>(value -> ++value));

    public static final Processor<Integer, Integer> ADD_TEN_PROCESSOR =
            new SimpleProcessor<>(value -> value + 10);

    public static final IdentifiedByConstProcessor<String, Integer, Integer> SUB_PROCESSOR =
            new IdentifiedByConstProcessor<>("SUB", new SimpleProcessor<>(value -> --value));

    public static final SimpleProcessor<Integer, String> KEY_PROCESSOR =
            new SimpleProcessor<>(
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
        ProcessorMap<String, Integer, Integer> processor =
                new ProcessorMap<>(
                        List.of(ADD_PROCESSOR, SUB_PROCESSOR), KEY_PROCESSOR, ADD_TEN_PROCESSOR);
        assertEquals(11, processor.process(10).value());
        assertEquals(-11, processor.process(-10).value());
        assertEquals(10, processor.process(0).value());
    }

    @Test
    public void shouldProcessWithDefaultProcessor() {}

    @Test
    public void keyExtractionErrorDoesNotCallDefault() {
        IllegalStateException failure = new IllegalStateException("broken parse");
        AtomicBoolean defaultCalled = new AtomicBoolean();
        ProcessorMap<String, Integer, Integer> processor =
                new ProcessorMap<>(
                        List.of(ADD_PROCESSOR),
                        new SimpleProcessor<>(
                                value -> {
                                    throw failure;
                                }),
                        value -> {
                            defaultCalled.set(true);
                            return Processed.of(value);
                        });

        Processed<Integer> result = processor.process(5);

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        ProcessorMap<String, Integer, Integer> processor =
                new ProcessorMap<>(
                        List.of(ADD_PROCESSOR),
                        new SimpleProcessor<>(value -> null),
                        value -> {
                            defaultCalled.set(true);
                            return Processed.of(value + 10);
                        });

        assertEquals(15, processor.process(5).value());
        assertTrue(defaultCalled.get());
    }
}
