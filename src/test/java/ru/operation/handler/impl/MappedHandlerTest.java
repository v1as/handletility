package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.processor.impl.SimpleProcessor;

public class MappedHandlerTest {

    private final int[] sum = {0};
    private final Handler<String> handler =
            new MappedHandler<>(
                    new SimpleProcessor<>(Integer::valueOf),
                    new SimpleHandler<>((Integer value) -> sum[0] = sum[0] + value));

    @Test
    public void shouldHandle() {
        assertTrue(handler.handle("2").isHandled());
        assertEquals(2, sum[0]);
    }

    @Test
    public void shouldErrorOnMapper() {
        Handled handled = handler.handle("abc");
        assertTrue(handled.isError());
        assertEquals(
                "Handling error because of mapping: ERROR: NumberFormatException: For input string:"
                        + " \"abc\"",
                handled.exception().getMessage());
    }
}
