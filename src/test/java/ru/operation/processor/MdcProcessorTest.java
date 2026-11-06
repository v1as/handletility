package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class MdcProcessorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void putsMdcValueAroundDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        Processor<String, String> processor =
                ProcessorBuilder.from(
                                Processor.<String, String>of(
                                        input -> {
                                            seen.set(MDC.get("traceId"));
                                            return input.toUpperCase();
                                        }))
                        .mdc("traceId", input -> "t-" + input)
                        .build();

        Processed<String> result = processor.process("ab");

        assertTrue(result.isDone());
        assertEquals("AB", result.value());
        assertEquals("t-ab", seen.get());
        assertNull(MDC.get("traceId"));
    }
}
