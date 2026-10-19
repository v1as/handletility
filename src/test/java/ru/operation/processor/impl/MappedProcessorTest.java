package ru.operation.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class MappedProcessorTest {

    private final Processor<String, Integer> processor =
            new MappedProcessor<>(
                    new SimpleProcessor<>(Integer::valueOf),
                    new SimpleProcessor<>((Integer value) -> 2 + value));

    @Test
    public void shouldProcess() {
        Processed<Integer> processed = processor.process("3");
        assertEquals(5, processed.value());
    }

    @Test
    public void shouldPropagateMapperError() {
        Processed<Integer> processed = processor.process("abc");
        assertTrue(processed.isError());
        assertInstanceOf(NumberFormatException.class, processed.exception());
    }

    @Test
    public void shouldPropagateMapperSkip() {
        Processor<String, Integer> skipping =
                new MappedProcessor<>(
                        input -> Processed.skipped(), new SimpleProcessor<>((Integer n) -> n + 1));

        assertTrue(skipping.process("x").isSkipped());
    }
}
