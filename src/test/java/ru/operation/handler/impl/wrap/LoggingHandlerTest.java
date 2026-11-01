package ru.operation.handler.impl.wrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import ru.operation.handler.Handler;
import ru.operation.handler.impl.ConsumerHandler;

public class LoggingHandlerTest {

    @Test
    public void delegatesResult() {
        Handler<String> logging = new LoggingHandler<>(new ConsumerHandler<>(s -> {}), "op");
        assertTrue(logging.handle("x").isDone());
    }

    @Test
    public void acceptsCustomLevel() {
        Handler<String> logging =
                new LoggingHandler<>(new ConsumerHandler<>(s -> {}), "op", Level.TRACE);
        assertTrue(logging.handle("x").isDone());
        assertTrue(logging.toString().startsWith("Logging(TRACE,"));
    }
}
