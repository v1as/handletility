package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.handler.Handled;
import ru.operation.processor.impl.SimpleProcessor;

public class MdcHandlerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void putsMdcValueAroundDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        var handler =
                new MdcHandler<>(
                        "requestId",
                        new SimpleProcessor<>(input -> "req-" + input),
                        new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))));

        Handled result = handler.handle("42");

        assertTrue(result.isHandled());
        assertEquals("req-42", seen.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void restoresPreviousMdcValue() {
        MDC.put("requestId", "old");
        AtomicReference<String> seen = new AtomicReference<>();
        var handler =
                new MdcHandler<>(
                        "requestId",
                        new SimpleProcessor<>(input -> "new"),
                        new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))));

        handler.handle("x");

        assertEquals("new", seen.get());
        assertEquals("old", MDC.get("requestId"));
    }
}
