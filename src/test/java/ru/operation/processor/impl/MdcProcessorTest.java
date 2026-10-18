package ru.operation.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.processor.Processed;

public class MdcProcessorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void putsMdcValueAroundDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        var processor =
                new MdcProcessor<>(
                        "traceId",
                        new SimpleProcessor<>(input -> "t-" + input),
                        new SimpleProcessor<String, String>(
                                input -> {
                                    seen.set(MDC.get("traceId"));
                                    return input.toUpperCase();
                                }));

        Processed<String> result = processor.process("ab");

        assertTrue(result.isDone());
        assertEquals("AB", result.value());
        assertEquals("t-ab", seen.get());
        assertNull(MDC.get("traceId"));
    }
}
