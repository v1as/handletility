package ru.operation.handler.impl.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;
import ru.operation.handler.Handlers;
import ru.operation.handler.impl.IdentifiedByConstHandler;
import ru.operation.processor.impl.SimpleProcessor;

public class HandlerMapTest {

    private final int[] store = {-1};

    private final IdentifiedByConstHandler<String, String> firstHandler =
            new IdentifiedByConstHandler<>(
                    "1",
                    (input) -> {
                        store[0] = 1;
                        return ok();
                    });

    private final IdentifiedByConstHandler<String, String> secondHandler =
            new IdentifiedByConstHandler<>(
                    "2",
                    (input) -> {
                        store[0] = 2;
                        return ok();
                    });

    private final Handler<String> forthHandler =
            (input) -> {
                store[0] = 4;
                return ok();
            };

    private final SimpleProcessor<String, String> identityKey = new SimpleProcessor<>(s -> s);

    @Test
    public void shouldHandleWithNoDefault() {
        Handler<String> handler =
                Handlers.compose(
                                new HandlerMap<>(
                                        List.of(firstHandler, secondHandler), identityKey, null))
                        .identified("testHandler")
                        .logging()
                        .build();

        assertTrue(handler.handle("3").isSkipped());
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
