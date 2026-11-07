package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.skipped;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class AbstractProcessorTest {

    @Test
    public void checkRejectedSkipsInternal() {
        AtomicBoolean called = new AtomicBoolean();
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Processed<String> processInternal(String input) {
                        called.set(true);
                        return done(input);
                    }
                };

        assertTrue(processor.process("no").isSkipped());
        assertFalse(called.get());

        assertEquals("ok", processor.process("ok").value());
        assertTrue(called.get());
    }

    @Test
    public void checkExceptionBecomesErrorAndSkipsInternal() {
        IllegalStateException boom = new IllegalStateException("boom");
        AtomicBoolean called = new AtomicBoolean();
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected boolean check(String input) {
                        throw boom;
                    }

                    @Override
                    protected Processed<String> processInternal(String input) {
                        called.set(true);
                        return done(input);
                    }
                };

        Processed<String> result = processor.process("x");
        assertTrue(result.isError());
        assertSame(boom, result.exception());
        assertFalse(called.get());
    }

    @Test
    public void exceptionLoggedAtTrace() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected Processed<String> processInternal(String input) {
                        throw boom;
                    }
                };
        Logger logger = (Logger) LoggerFactory.getLogger(processor.getClass());
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            Processed<String> result = processor.process("x");
            assertTrue(result.isError());
            assertSame(boom, result.exception());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(1, appender.list.size());
        assertEquals(Level.TRACE, appender.list.get(0).getLevel());
        assertEquals("Processing error of 'x'", appender.list.get(0).getFormattedMessage());
    }

    @Test
    public void exceptionNotLoggedAboveTrace() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected Processed<String> processInternal(String input) {
                        throw boom;
                    }
                };
        Logger logger = (Logger) LoggerFactory.getLogger(processor.getClass());
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            assertTrue(processor.process("x").isError());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertTrue(appender.list.isEmpty());
    }

    @Test
    public void logsCheckSkipAndInternalResult() {
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Processed<String> processInternal(String input) {
                        return done(input);
                    }
                };
        Logger logger = (Logger) LoggerFactory.getLogger(processor.getClass());
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            assertTrue(processor.process("no").isSkipped());
            assertEquals("ok", processor.process("ok").value());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(
                Arrays.asList(
                        "Skipped because check rejected input: 'no'",
                        "Processed 'ok' to '[DONE: ok]'"),
                appender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList()));
    }

    @Test
    public void onResultReplacesInternalResult() {
        Processor<String, String> processor =
                new AbstractProcessor<String, String>() {
                    @Override
                    protected Processed<String> processInternal(String input) {
                        return done(input);
                    }

                    @Override
                    protected Processed<String> onResult(
                            String input, Processed<String> processed) {
                        return skipped();
                    }
                };

        assertTrue(processor.process("x").isSkipped());
    }
}
