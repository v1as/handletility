package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class AroundHandlerTest {

    @Test
    public void afterReceivesMainResultAndDoesNotReplaceIt() {
        List<String> order = new ArrayList<>();
        AtomicReference<Handled> seenAfter = new AtomicReference<>();
        Handler<String> around =
                AroundHandler.<String>builder()
                        .beforeHandler(new SimpleHandler<>((String s) -> order.add("before")))
                        .handler(
                                new SimpleHandler<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(
                                result -> {
                                    order.add("after");
                                    seenAfter.set(result);
                                    return Handled.error("ignored");
                                })
                        .build();

        Handled result = around.handle("x");

        assertTrue(result.isError());
        assertEquals("boom", result.exception().getMessage());
        assertSame(result, seenAfter.get());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void returnsMainHandlerResult() {
        Handler<String> around = AroundHandler.<String>builder().handler(input -> ok()).build();

        assertTrue(around.handle("ok").isHandled());
    }
}
