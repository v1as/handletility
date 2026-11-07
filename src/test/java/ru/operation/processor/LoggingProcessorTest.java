package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LoggingProcessorTest {

    @Test
    public void skippedOutcomeLoggedAtTrace() {
        Processor<String, String> logging =
                new LoggingProcessor<>(
                        (Processor<String, String>) input -> Processed.skipped(),
                        LoggingProcessorTest.class,
                        null,
                        Level.DEBUG);

        Logger logger = (Logger) LoggerFactory.getLogger(LoggingProcessorTest.class);
        ListAppender<ILoggingEvent> appender = attachAt(logger, ch.qos.logback.classic.Level.TRACE);
        try {
            assertTrue(logging.process("x").isSkipped());
        } finally {
            detach(logger, appender);
        }

        assertEquals(1, appender.list.size());
        assertEquals(ch.qos.logback.classic.Level.TRACE, appender.list.get(0).getLevel());
        assertEquals("Processed 'x' to '[SKIPPED]'", appender.list.get(0).getFormattedMessage());
    }

    @Test
    public void doneOutcomeUsesConfiguredLevel() {
        Processor<String, String> logging =
                new LoggingProcessor<>(
                        (Processor<String, String>) input -> Processed.done(input),
                        LoggingProcessorTest.class,
                        null,
                        Level.DEBUG);

        Logger logger = (Logger) LoggerFactory.getLogger(LoggingProcessorTest.class);
        ListAppender<ILoggingEvent> appender = attachAt(logger, ch.qos.logback.classic.Level.DEBUG);
        try {
            assertTrue(logging.process("x").isDone());
        } finally {
            detach(logger, appender);
        }

        assertEquals(1, appender.list.size());
        assertEquals(ch.qos.logback.classic.Level.DEBUG, appender.list.get(0).getLevel());
    }

    @Test
    public void skippedNotVisibleWhenLoggerAboveTrace() {
        Processor<String, String> logging =
                new LoggingProcessor<>(
                        (Processor<String, String>) input -> Processed.skipped(),
                        LoggingProcessorTest.class,
                        null,
                        Level.DEBUG);

        Logger logger = (Logger) LoggerFactory.getLogger(LoggingProcessorTest.class);
        ListAppender<ILoggingEvent> appender = attachAt(logger, ch.qos.logback.classic.Level.DEBUG);
        try {
            assertTrue(logging.process("x").isSkipped());
        } finally {
            detach(logger, appender);
        }

        assertTrue(appender.list.isEmpty());
    }

    private static ListAppender<ILoggingEvent> attachAt(
            Logger logger, ch.qos.logback.classic.Level level) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(level);
        return appender;
    }

    private static void detach(Logger logger, ListAppender<ILoggingEvent> appender) {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }
}
