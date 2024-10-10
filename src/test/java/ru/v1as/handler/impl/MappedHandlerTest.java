package ru.v1as.handler.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.processor.impl.SimpleProcessor;

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
