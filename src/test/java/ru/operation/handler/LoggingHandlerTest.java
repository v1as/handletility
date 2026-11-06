package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

public class LoggingHandlerTest {

    @Test
    public void delegatesResult() {
        Handler<String> logging =
                HandlerBuilder.from(Handler.<String>of(s -> {})).named("op").logging().build();
        assertTrue(logging.handle("x").isDone());
        assertTrue(logging.toString().startsWith("Logging(DEBUG,"));
    }

    @Test
    public void acceptsCustomLevel() {
        Handler<String> logging =
                HandlerBuilder.from(Handler.<String>of(s -> {}))
                        .named("op")
                        .logging(Level.TRACE)
                        .build();
        assertTrue(logging.handle("x").isDone());
        assertTrue(logging.toString().contains("Logging(TRACE,"));
    }
}
