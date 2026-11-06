package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;

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

public class SwitchProcessorTest {

    public static final IdentifiedProcessor<String, Integer, Integer> ADD_PROCESSOR =
            Processor.<Integer, Integer>of(value -> ++value).identifiedBy("ADD");

    public static final Processor<Integer, Integer> ADD_TEN_PROCESSOR =
            Processor.of(value -> value + 10);

    public static final IdentifiedProcessor<String, Integer, Integer> SUB_PROCESSOR =
            Processor.<Integer, Integer>of(value -> --value).identifiedBy("SUB");

    public static final Processor<Integer, String> KEY_READER =
            Processor.of(
                    value -> {
                        if (value > 0) {
                            return "ADD";
                        } else if (value < 0) {
                            return "SUB";
                        }
                        return "ZERO";
                    });

    @Test
    public void shouldProcessWithoutDefaultProcessor() {
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .processor(SUB_PROCESSOR)
                        .keyReader(KEY_READER)
                        .defaultProcessor(ADD_TEN_PROCESSOR)
                        .build();
        assertEquals(11, processor.process(10).value());
        assertEquals(-11, processor.process(-10).value());
        assertEquals(10, processor.process(0).value());
    }

    @Test
    public void shouldProcessWithDefaultProcessor() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(KEY_READER)
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value + 100);
                                })
                        .build();

        assertEquals(4, processor.process(3).value());
        assertFalse(defaultCalled.get());

        assertEquals(100, processor.process(0).value());
        assertTrue(defaultCalled.get());
    }

    @Test
    public void keyReaderErrorDoesNotCallDefault() {
        IllegalStateException failure = new IllegalStateException("broken parse");
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(
                                Processor.of(
                                        value -> {
                                            throw failure;
                                        }))
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value);
                                })
                        .build();

        Processed<Integer> result = processor.process(5);

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyReaderUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchProcessor<String, Integer, Integer> processor =
                SwitchProcessor.<String, Integer, Integer>switchProcessor()
                        .processor(ADD_PROCESSOR)
                        .keyReader(Processor.of(value -> null))
                        .defaultProcessor(
                                value -> {
                                    defaultCalled.set(true);
                                    return done(value + 10);
                                })
                        .build();

        assertEquals(15, processor.process(5).value());
        assertTrue(defaultCalled.get());
    }

    @Test
    public void traceLogsInputOnceAndBranchOutcome() {
        Logger logger = (Logger) LoggerFactory.getLogger(SwitchProcessor.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            SwitchProcessor<String, Integer, Integer> processor =
                    SwitchProcessor.<String, Integer, Integer>switchProcessor()
                            .processor(ADD_PROCESSOR)
                            .keyReader(KEY_READER)
                            .build();

            assertEquals(11, processor.process(10).value());
            assertEquals(
                    Arrays.asList(
                            "Handling input '10'",
                            "Processor 'ConstantIdentifiedProcessor' (ADD) has result '[DONE: 11]'",
                            "Processed '10' to '[DONE: 11]'"),
                    messages(appender));

            appender.list.clear();
            assertTrue(processor.process(0).isSkipped());
            assertEquals(
                    Arrays.asList(
                            "Handling input '0'",
                            "No processor matched, result '[SKIPPED]'",
                            "Processed '0' to '[SKIPPED]'"),
                    messages(appender));

            appender.list.clear();
            IllegalStateException failure = new IllegalStateException("broken parse");
            SwitchProcessor<String, Integer, Integer> broken =
                    SwitchProcessor.<String, Integer, Integer>switchProcessor()
                            .processor(ADD_PROCESSOR)
                            .keyReader(
                                    Processor.of(
                                            value -> {
                                                throw failure;
                                            }))
                            .build();
            assertSame(failure, broken.process(5).exception());
            assertEquals(
                    Arrays.asList(
                            "Handling input '5'",
                            "Key read failed",
                            "Processed '5' to '[ERROR: IllegalStateException: broken parse]'"),
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
