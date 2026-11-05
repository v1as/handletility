package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.MdcEntry;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerBuilder;
import ru.operation.processor.impl.FunctionProcessor;

public class MdcHandlerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void putsMdcValueAroundDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        MdcHandler<String> handler =
                new MdcHandler<>(
                        "requestId",
                        input -> "req-" + input,
                        new ConsumerHandler<String>(input -> seen.set(MDC.get("requestId"))));

        Handled result = handler.handle("42");

        assertTrue(result.isDone());
        assertEquals("req-42", seen.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void restoresPreviousMdcValue() {
        MDC.put("requestId", "old");
        AtomicReference<String> seen = new AtomicReference<>();
        MdcHandler<String> handler =
                new MdcHandler<>(
                        "requestId",
                        input -> "new",
                        new ConsumerHandler<String>(input -> seen.set(MDC.get("requestId"))));

        handler.handle("x");

        assertEquals("new", seen.get());
        assertEquals("old", MDC.get("requestId"));
    }

    @Test
    public void putsMultipleMdcKeys() {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();
        MdcHandler<String> handler =
                new MdcHandler<>(
                        Arrays.asList(
                                MdcEntry.of("requestId", s -> "r-" + s),
                                new MdcEntry<>("userId", new FunctionProcessor<>(s -> "u-" + s))),
                        new ConsumerHandler<String>(
                                input -> {
                                    requestId.set(MDC.get("requestId"));
                                    userId.set(MDC.get("userId"));
                                }));

        assertTrue(handler.handle("1").isDone());
        assertEquals("r-1", requestId.get());
        assertEquals("u-1", userId.get());
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("userId"));
    }

    @Test
    public void builderAccumulatesMultipleMdcCalls() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();
        Handler<String> handler =
                HandlerBuilder.from(
                                new ConsumerHandler<String>(
                                        input -> {
                                            a.set(MDC.get("a"));
                                            b.set(MDC.get("b"));
                                        }))
                        .mdc("a", s -> "A")
                        .mdc("b", s -> "B")
                        .build();

        assertTrue(handler.handle("x").isDone());
        assertEquals("A", a.get());
        assertEquals("B", b.get());
        assertTrue(handler.toString().contains("Mdc([a, b],"));
    }

    @Test
    public void acceptsMethodReference() {
        AtomicReference<String> seen = new AtomicReference<>();
        MdcHandler<String> handler =
                new MdcHandler<>(
                        "requestId",
                        String::toUpperCase,
                        new ConsumerHandler<String>(input -> seen.set(MDC.get("requestId"))));

        assertTrue(handler.handle("ab").isDone());
        assertEquals("AB", seen.get());
    }
}
