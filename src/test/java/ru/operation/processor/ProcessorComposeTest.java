package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.processor.impl.MdcProcessor;
import ru.operation.processor.impl.SimpleProcessor;
import ru.operation.processor.impl.wrap.LoggingProcessor;
import ru.operation.util.function.ThrowableConsumer;
import ru.operation.util.function.ThrowableFunction;

public class ProcessorComposeTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void buildOrderIsLoggingMdcCore() {
        Processor<String, String> core = new SimpleProcessor<>(String::toUpperCase);
        Processor<String, String> composed =
                Processors.compose(core)
                        .mdc("traceId", s -> "t-" + s)
                        .logging()
                        .named("upper-log")
                        .build();

        assertInstanceOf(MdcProcessor.class, composed);
        assertTrue(composed.toString().startsWith("Mdc([traceId], Logging(DEBUG, Catching("));
    }

    @Test
    public void loggingWithoutName() {
        Processor<String, String> composed =
                Processors.compose(new SimpleProcessor<String, String>(s -> s)).logging().build();

        assertInstanceOf(LoggingProcessor.class, composed);
        assertTrue(composed.toString().startsWith("Logging(DEBUG, Catching("));
    }

    @Test
    public void processesWithMdc() {
        AtomicReference<String> seen = new AtomicReference<>();
        Processor<String, String> composed =
                Processors.compose(
                                new SimpleProcessor<String, String>(
                                        input -> {
                                            seen.set(MDC.get("traceId"));
                                            return input;
                                        }))
                        .mdc("traceId", s -> "v")
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("x", result.value());
        assertEquals("v", seen.get());
    }

    @Test
    public void beforeAndAfterWrapCoreInsideMdc() {
        List<String> order = new ArrayList<>();
        Processor<String, String> composed =
                Processors.compose(
                                new SimpleProcessor<String, String>(
                                        s -> {
                                            order.add("main");
                                            return s.toUpperCase();
                                        }))
                        .before(new SimpleHandler<>(s -> order.add("before")))
                        .after(
                                result -> {
                                    order.add("after");
                                    return ok();
                                })
                        .mdc("k", s -> "v")
                        .build();

        assertEquals("X", composed.process("x").value());
        assertEquals(List.of("before", "main", "after"), order);
        assertTrue(composed.toString().contains("Around("));
        assertTrue(composed.toString().contains("Catching("));
        assertTrue(composed.toString().contains("Mdc([k],"));
        assertTrue(composed.toString().indexOf("Mdc(") < composed.toString().indexOf("Catching("));
        assertTrue(
                composed.toString().indexOf("Catching(") < composed.toString().indexOf("Around("));
    }

    @Test
    public void composeAcceptsFunctionAndBeforeAfterConsumers() {
        List<String> order = new ArrayList<>();
        Processor<String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    order.add("main");
                                    return s.toUpperCase();
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Processed<String> result) -> {
                                    order.add("after");
                                })
                        .build();

        assertEquals("X", composed.process("x").value());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void rawProcessorThrowBecomesError() {
        Processor<String, String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        Processor<String, String> composed = Processors.compose(raw).build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertEquals("boom", result.exception().getMessage());
    }

    @Test
    public void onExceptionCanRecover() {
        Processor<String, String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        Processor<String, String> composed =
                Processors.compose(raw).onException(failed -> Processed.of("recovered")).build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
    }

    @Test
    public void composeFunctionStillMapsExceptionToError() {
        Processor<String, String> composed =
                Processors.compose(
                                Processors.<String, String>of(
                                        s -> {
                                            throw new RuntimeException("from-function");
                                        }))
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isError());
        assertEquals("from-function", result.exception().getMessage());
    }

    @Test
    public void functionOnExceptionReceivesOriginalCheckedException() {
        ThrowableFunction<String, String> transform =
                s -> {
                    throw new IOException("checked");
                };
        Processor<String, String> composed =
                Processors.compose(transform)
                        .onException(
                                failed -> {
                                    assertInstanceOf(IOException.class, failed.exception());
                                    assertEquals("checked", failed.exception().getMessage());
                                    return Processed.of("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
    }

    @Test
    public void beforeConsumerThrowSkipsCoreAndReachesOnException() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicReference<Exception> seen = new AtomicReference<>();
        ThrowableConsumer<String> before =
                s -> {
                    throw new IllegalStateException("hook");
                };
        Processor<String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    coreCalled.set(true);
                                    return s;
                                })
                        .before(before)
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Processed.of("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
        assertFalse(coreCalled.get());
        assertInstanceOf(IllegalStateException.class, seen.get());
        assertEquals("hook", seen.get().getMessage());
    }

    @Test
    public void afterConsumerThrowReplacesCoreResult() {
        AtomicReference<Exception> seen = new AtomicReference<>();
        ThrowableConsumer<Processed<String>> after =
                result -> {
                    throw new IllegalStateException("after");
                };
        Processor<String, String> composed =
                Processors.compose((String s) -> "ok")
                        .after(after)
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Processed.of("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
        assertEquals("after", seen.get().getMessage());
    }

    @Test
    public void whenFalseSkipsBeforeCoreAndAfter() {
        List<String> order = new ArrayList<>();
        AtomicBoolean coreCalled = new AtomicBoolean();
        Processor<String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    coreCalled.set(true);
                                    return s.toUpperCase();
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Processed<String> result) -> {
                                    order.add("after");
                                })
                        .when(s -> false)
                        .build();

        assertTrue(composed.process("x").isSkipped());
        assertFalse(coreCalled.get());
        assertEquals(List.of(), order);
        assertTrue(composed.toString().contains("Catching(When(Around("));
    }

    @Test
    public void whenTrueRunsBeforeCoreAndAfter() {
        List<String> order = new ArrayList<>();
        Processor<String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    order.add("main");
                                    return s.toUpperCase();
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Processed<String> result) -> {
                                    order.add("after");
                                })
                        .when(s -> "x".equals(s))
                        .build();

        assertEquals("X", composed.process("x").value());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void whenPredicateExceptionReachesOnException() {
        AtomicReference<Exception> seen = new AtomicReference<>();
        Processor<String, String> composed =
                Processors.compose((String s) -> s)
                        .when(
                                s -> {
                                    throw new IOException("pred");
                                })
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Processed.of("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
        assertInstanceOf(IOException.class, seen.get());
        assertEquals("pred", seen.get().getMessage());
    }

    @Test
    public void whenWithoutAroundStillWrapsCore() {
        Processor<String, String> composed =
                Processors.compose((String s) -> s).when(s -> true).build();

        assertEquals("x", composed.process("x").value());
        assertTrue(composed.toString().contains("Catching(When("));
        assertFalse(composed.toString().contains("Around("));
    }

    @Test
    public void secondWhenReplacesFirst() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        Processor<String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    coreCalled.set(true);
                                    return s;
                                })
                        .when(s -> false)
                        .when(s -> true)
                        .build();

        assertEquals("x", composed.process("x").value());
        assertTrue(coreCalled.get());
    }

    @Test
    public void composeFunctionNullIsDoneWithoutPayload() {
        ThrowableFunction<String, String> transform = s -> null;
        Processor<String, String> composed = Processors.compose(transform).build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertFalse(result.isSkipped());
        assertFalse(result.hasValue());
    }

    @Test
    public void loggingUsesCoreClassNotCatchingWrapper() {
        Processor<String, Integer> composed =
                Processors.compose(new ParseAmountProcessor()).named("amount").logging().build();

        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            assertEquals(7, composed.process("7").value());
        } finally {
            root.detachAppender(appender);
        }

        assertEquals(
                ParseAmountProcessor.class.getName() + "[amount]",
                appender.list.stream()
                        .filter(event -> event.getFormattedMessage().startsWith("Processed"))
                        .map(ILoggingEvent::getLoggerName)
                        .findFirst()
                        .orElseThrow());
    }

    private static final class ParseAmountProcessor extends SimpleProcessor<String, Integer> {

        private ParseAmountProcessor() {
            super(Integer::valueOf);
        }
    }
}
