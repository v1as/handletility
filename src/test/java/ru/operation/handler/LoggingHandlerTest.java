package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.skipped;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
    public void skippedOutcomeLoggedAtTrace() {
        Handler<String> logging =
                new LoggingHandler<>(
                        input -> skipped(), LoggingHandlerTest.class, null, Level.DEBUG);

        Logger logger = (Logger) LoggerFactory.getLogger(LoggingHandlerTest.class);
        ch.qos.logback.classic.Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(ch.qos.logback.classic.Level.TRACE);
        try {
            assertTrue(logging.handle("x").isSkipped());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(1, appender.list.size());
        assertEquals(ch.qos.logback.classic.Level.TRACE, appender.list.get(0).getLevel());
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
