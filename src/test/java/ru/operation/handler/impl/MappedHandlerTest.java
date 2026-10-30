package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.Processed;
import ru.operation.processor.impl.SimpleProcessor;

public class MappedHandlerTest {

    private final int[] sum = {0};
    private final Handler<String> handler =
            new MappedHandler<>(
                    new SimpleProcessor<>(Integer::valueOf),
                    new SimpleHandler<>((Integer value) -> sum[0] = sum[0] + value));

    @Test
    public void shouldHandle() {
        assertTrue(handler.handle("2").isDone());
        assertEquals(2, sum[0]);
    }

    @Test
    public void shouldPropagateMapperError() {
        Handled handled = handler.handle("abc");
        assertTrue(handled.isError());
        assertInstanceOf(NumberFormatException.class, handled.exception());
    }

    @Test
    public void shouldPropagateMapperSkip() {
        Handler<String> skipping =
                new MappedHandler<>(
                        input -> Processed.skipped(),
                        new SimpleHandler<>((Integer value) -> sum[0] = value));

        assertTrue(skipping.handle("x").isSkipped());
        assertEquals(0, sum[0]);
    }
}
