package ru.operation.processor.impl.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import ru.operation.processor.IdentifiedProcessor;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.FunctionProcessor;

public class RoutingProcessorTest {

    public static final IdentifiedProcessor<String, Integer, Integer> ADD_PROCESSOR =
            new FunctionProcessor<Integer, Integer>(value -> ++value).identifiedBy("ADD");

    public static final Processor<Integer, Integer> ADD_TEN_PROCESSOR =
            new FunctionProcessor<>(value -> value + 10);

    public static final IdentifiedProcessor<String, Integer, Integer> SUB_PROCESSOR =
            new FunctionProcessor<Integer, Integer>(value -> --value).identifiedBy("SUB");

    public static final FunctionProcessor<Integer, String> KEY_PROCESSOR =
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
        RoutingProcessor<String, Integer, Integer> processor =
                new RoutingProcessor<>(
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
        RoutingProcessor<String, Integer, Integer> processor =
                new RoutingProcessor<>(
                        List.of(ADD_PROCESSOR),
                        new FunctionProcessor<>(
                                value -> {
                                    throw failure;
                                }),
                        value -> {
                            defaultCalled.set(true);
                            return done(value);
                        });

        Processed<Integer> result = processor.process(5);

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        RoutingProcessor<String, Integer, Integer> processor =
                new RoutingProcessor<>(
                        List.of(ADD_PROCESSOR),
                        new FunctionProcessor<>(value -> null),
                        value -> {
                            defaultCalled.set(true);
                            return done(value + 10);
                        });

        assertEquals(15, processor.process(5).value());
        assertTrue(defaultCalled.get());
    }
}
