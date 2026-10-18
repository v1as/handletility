package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.MdcEntry;
import ru.operation.handler.Handled;
import ru.operation.handler.Handlers;
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
                        input -> "req-" + input,
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
                        input -> "new",
                        new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))));

        handler.handle("x");

        assertEquals("new", seen.get());
        assertEquals("old", MDC.get("requestId"));
    }

    @Test
    public void putsMultipleMdcKeys() {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();
        var handler =
                new MdcHandler<>(
                        List.of(
                                MdcEntry.of("requestId", s -> "r-" + s),
                                new MdcEntry<>("userId", new SimpleProcessor<>(s -> "u-" + s))),
                        new SimpleHandler<String>(
                                input -> {
                                    requestId.set(MDC.get("requestId"));
                                    userId.set(MDC.get("userId"));
                                }));

        assertTrue(handler.handle("1").isHandled());
        assertEquals("r-1", requestId.get());
        assertEquals("u-1", userId.get());
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("userId"));
    }

    @Test
    public void composeAccumulatesMultipleMdcCalls() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();
        var handler =
                Handlers.compose(
                                new SimpleHandler<String>(
                                        input -> {
                                            a.set(MDC.get("a"));
                                            b.set(MDC.get("b"));
                                        }))
                        .identified("multi-mdc")
                        .mdc("a", s -> "A")
                        .mdc("b", s -> "B")
                        .build();

        assertTrue(handler.handle("x").isHandled());
        assertEquals("A", a.get());
        assertEquals("B", b.get());
        assertTrue(handler.toString().contains("Mdc([a, b],"));
        assertEquals("multi-mdc", handler.getIdentifier());
    }

    @Test
    public void acceptsMethodReference() {
        AtomicReference<String> seen = new AtomicReference<>();
        var handler =
                new MdcHandler<>(
                        "requestId",
                        String::toUpperCase,
                        new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))));

        assertTrue(handler.handle("ab").isHandled());
        assertEquals("AB", seen.get());
    }
}
