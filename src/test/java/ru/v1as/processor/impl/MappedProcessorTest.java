package ru.v1as.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.v1as.processor.Processed;
import ru.v1as.processor.Processor;

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
    public void shouldErrorWhileMapping() {
        Processed<Integer> processed = processor.process("abc");
        assertTrue(processed.isError());
        assertEquals(
                "Processing error because of mapping ERROR: NumberFormatException: For input"
                        + " string: \"abc\"",
                processed.exception().getMessage());
    }
}
