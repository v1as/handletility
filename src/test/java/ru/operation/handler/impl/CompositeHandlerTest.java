package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.handled;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class CompositeHandlerTest {

    @Test
    public void runsBeforeHandlerAndAfterEvenOnError() {
        List<String> order = new ArrayList<>();
        Handler<String> composite =
                CompositeHandler.<String>builder()
                        .beforeHandler(new SimpleHandler<>(s -> order.add("before")))
                        .handler(
                                new SimpleHandler<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(new SimpleHandler<>(s -> order.add("after")))
                        .build();

        Handled result = composite.handle("x");

        assertTrue(result.isError());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void returnsMainHandlerResult() {
        Handler<String> composite =
                CompositeHandler.<String>builder().handler(input -> handled()).build();

        assertTrue(composite.handle("ok").isHandled());
    }
}
