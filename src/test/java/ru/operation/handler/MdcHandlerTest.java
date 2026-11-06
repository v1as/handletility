package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.MdcEntry;
import ru.operation.processor.Processor;

public class MdcHandlerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void putsMdcValueAroundDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        Handler<String> handler =
                HandlerBuilder.from(Handler.<String>of(input -> seen.set(MDC.get("requestId"))))
                        .mdc("requestId", input -> "req-" + input)
                        .build();

        Handled result = handler.handle("42");

        assertTrue(result.isDone());
        assertEquals("req-42", seen.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void restoresPreviousMdcValue() {
        MDC.put("requestId", "old");
        AtomicReference<String> seen = new AtomicReference<>();
        Handler<String> handler =
                HandlerBuilder.from(Handler.<String>of(input -> seen.set(MDC.get("requestId"))))
                        .mdc("requestId", input -> "new")
                        .build();

        handler.handle("x");

        assertEquals("new", seen.get());
        assertEquals("old", MDC.get("requestId"));
    }

    @Test
    public void putsMultipleMdcKeys() {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();
        Handler<String> handler =
                HandlerBuilder.from(
                                Handler.<String>of(
                                        input -> {
                                            requestId.set(MDC.get("requestId"));
                                            userId.set(MDC.get("userId"));
                                        }))
                        .mdc(
                                Arrays.asList(
                                        MdcEntry.of("requestId", s -> "r-" + s),
                                        new MdcEntry<>("userId", Processor.of(s -> "u-" + s))))
                        .build();

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
                                Handler.<String>of(
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
        Handler<String> handler =
                HandlerBuilder.from(Handler.<String>of(input -> seen.set(MDC.get("requestId"))))
                        .mdc("requestId", String::toUpperCase)
                        .build();

        assertTrue(handler.handle("ab").isDone());
        assertEquals("AB", seen.get());
    }
}
