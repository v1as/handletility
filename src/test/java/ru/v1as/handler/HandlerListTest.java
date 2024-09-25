package ru.v1as.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import java.util.List;
import org.junit.jupiter.api.Test;

public class HandlerListTest {

    @Test
    public void should_process_after_skip() {
        Handler<String> handlers =
                new HandlerList<>("test-handler", List.of(value -> skipped(), value -> handled()));
        Handled result = handlers.handle("hello");
        assertEquals(handled(), result);
    }
}
