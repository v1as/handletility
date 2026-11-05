package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.skipped;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class AbstractHandlerTest {

    @Test
    public void checkRejectedSkipsInternal() {
        AtomicBoolean called = new AtomicBoolean();
        Handler<String> handler =
                new AbstractHandler<String>() {
                    @Override
                    public boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Handled handleInternal(String input) {
                        called.set(true);
                        return done();
                    }
                };

        assertTrue(handler.handle("no").isSkipped());
        assertFalse(called.get());

        assertTrue(handler.handle("ok").isDone());
        assertTrue(called.get());
    }

    @Test
    public void checkExceptionBecomesErrorAndSkipsInternal() {
        IllegalStateException boom = new IllegalStateException("boom");
        AtomicBoolean called = new AtomicBoolean();
        Handler<String> handler =
                new AbstractHandler<String>() {
                    @Override
                    public boolean check(String input) {
                        throw boom;
                    }

                    @Override
                    protected Handled handleInternal(String input) {
                        called.set(true);
                        return done();
                    }
                };

        Handled result = handler.handle("x");
        assertTrue(result.isError());
        assertSame(boom, result.exception());
        assertFalse(called.get());
    }

    @Test
    public void logsCheckSkipAndInternalResult() {
        Handler<String> handler =
                new AbstractHandler<String>() {
                    @Override
                    public boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Handled handleInternal(String input) {
                        return done();
                    }
                };
        Logger logger = (Logger) LoggerFactory.getLogger(handler.getClass());
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            assertTrue(handler.handle("no").isSkipped());
            assertTrue(handler.handle("ok").isDone());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(
                Arrays.asList(
                        "Skipped because check rejected input: 'no'",
                        "Handled input 'ok' with result [DONE]"),
                appender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList()));
    }

    @Test
    public void onResultReplacesInternalResult() {
        Handler<String> handler =
                new AbstractHandler<String>() {
                    @Override
                    protected Handled handleInternal(String input) {
                        return done();
                    }

                    @Override
                    protected Handled onResult(String input, Handled handled) {
                        return skipped();
                    }
                };

        assertTrue(handler.handle("x").isSkipped());
    }
}
