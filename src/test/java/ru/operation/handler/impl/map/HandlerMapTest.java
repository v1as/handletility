package ru.operation.handler.impl.map;

import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.handled;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;
import ru.operation.handler.impl.IdentifiedByConstHandler;
import ru.operation.processor.impl.SimpleProcessor;

public class HandlerMapTest {

    private final int[] store = {-1};

    private final IdentifiedByConstHandler<String, String> firstHandler =
            new IdentifiedByConstHandler<>(
                    "1",
                    (input) -> {
                        store[0] = 1;
                        return handled();
                    });

    private final IdentifiedByConstHandler<String, String> secondHandler =
            new IdentifiedByConstHandler<>(
                    "2",
                    (input) -> {
                        store[0] = 2;
                        return handled();
                    });

    private final Handler<String> forthHandler =
            (input) -> {
                store[0] = 4;
                return handled();
            };

    private final SimpleProcessor<String, String> identityKey = new SimpleProcessor<>(identity());

    @Test
    public void shouldHandleWithNoDefault() {
        HandlerMap<String, String> handler =
                new HandlerMap<>(List.of(firstHandler, secondHandler), identityKey, null);

        assertTrue(handler.handle("3").isError());
        assertEquals(-1, store[0]);

        assertTrue(handler.handle("1").isHandled());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isHandled());
        assertEquals(2, store[0]);
    }

    @Test
    public void shouldHandleWithDefault() {
        HandlerMap<String, String> handler =
                new HandlerMap<>(List.of(firstHandler, secondHandler), identityKey, forthHandler);

        assertTrue(handler.handle("abc").isHandled());
        assertEquals(4, store[0]);

        assertTrue(handler.handle("1").isHandled());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isHandled());
        assertEquals(2, store[0]);
    }
}
