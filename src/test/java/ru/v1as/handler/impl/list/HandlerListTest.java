package ru.v1as.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;

public class HandlerListTest {

    @Test
    public void should_process_after_skip() {
        Handler<String> handlers =
                new HandlerList<>(List.of(value -> skipped(), value -> handled()));
        Handled result = handlers.handle("hello");
        assertEquals(handled(), result);
    }
}
