package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.operation.processor.Processor;

public class SwitchHandlerTest {

    private final int[] store = {-1};

    private final IdentifiedHandler<String, String> firstHandler = marking(1).identifiedBy("1");

    private final IdentifiedHandler<String, String> secondHandler = marking(2).identifiedBy("2");

    private final Handler<String> forthHandler =
            (input) -> {
                store[0] = 4;
                return done();
            };

    private final Processor<String, String> identityKeyReader = Processor.of(s -> s);

    private Handler<String> marking(int mark) {
        return input -> {
            store[0] = mark;
            return done();
        };
    }

    @Test
    public void shouldHandleWithNoDefault() {
        Handler<String> handler =
                HandlerBuilder.from(
                                SwitchHandler.<String, String>switchHandler()
                                        .handler(firstHandler)
                                        .handler(secondHandler)
                                        .keyReader(identityKeyReader)
                                        .build())
                        .logging()
                        .build();

        assertTrue(handler.handle("3").isSkipped());
        assertEquals(-1, store[0]);

        assertTrue(handler.handle("1").isDone());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isDone());
        assertEquals(2, store[0]);
    }

    @Test
    public void shouldHandleWithDefault() {
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .handler(secondHandler)
                        .keyReader(identityKeyReader)
                        .defaultHandler(forthHandler)
                        .build();

        assertTrue(handler.handle("abc").isDone());
        assertEquals(4, store[0]);

        assertTrue(handler.handle("1").isDone());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isDone());
        assertEquals(2, store[0]);
    }

    @Test
    public void keyReaderErrorDoesNotCallDefault() {
        IllegalStateException failure = new IllegalStateException("broken parse");
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .keyReader(
                                Processor.of(
                                        input -> {
                                            throw failure;
                                        }))
                        .defaultHandler(
                                input -> {
                                    defaultCalled.set(true);
                                    return done();
                                })
                        .build();

        Handled result = handler.handle("weird");

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyReaderUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .keyReader(Processor.of(input -> null))
                        .defaultHandler(
                                input -> {
                                    defaultCalled.set(true);
                                    return done();
                                })
                        .build();

        assertTrue(handler.handle("weird").isDone());
        assertTrue(defaultCalled.get());
    }

    @Test
    public void traceLogsInputOnceAndBranchOutcome() {
        Logger logger = (Logger) LoggerFactory.getLogger(SwitchHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            SwitchHandler<String, String> handler =
                    SwitchHandler.<String, String>switchHandler()
                            .handler(firstHandler)
                            .keyReader(identityKeyReader)
                            .build();

            assertTrue(handler.handle("1").isDone());
            assertEquals(
                    Arrays.asList(
                            "Handling input '1'",
                            "Handler 'ConstantIdentifiedHandler' (1) has result '[DONE]'",
                            "Handled input '1' with result [DONE]"),
                    messages(appender));

            appender.list.clear();
            assertTrue(handler.handle("missing").isSkipped());
            assertEquals(
                    Arrays.asList(
                            "Handling input 'missing'",
                            "No handler matched, result '[SKIPPED]'",
                            "Handled input 'missing' with result [SKIPPED]"),
                    messages(appender));

            appender.list.clear();
            IllegalStateException failure = new IllegalStateException("broken parse");
            SwitchHandler<String, String> broken =
                    SwitchHandler.<String, String>switchHandler()
                            .handler(firstHandler)
                            .keyReader(
                                    Processor.of(
                                            input -> {
                                                throw failure;
                                            }))
                            .build();
            assertSame(failure, broken.handle("weird").exception());
            assertEquals(
                    Arrays.asList(
                            "Handling input 'weird'",
                            "Key read failed",
                            "Handled input 'weird' with result [ERROR: class"
                                    + " java.lang.IllegalStateException broken parse]"),
                    messages(appender));
            assertEquals("broken parse", appender.list.get(1).getThrowableProxy().getMessage());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }

    private static List<String> messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
