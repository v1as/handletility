package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.util.function.ThrowableConsumer;
import ru.operation.util.function.ThrowableFunction;

public class ProcessorBuilderTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void buildOrderIsLoggingMdcCore() {
        Processor<String, String> core = Processor.of(String::toUpperCase);
        Processor<String, String> composed =
                ProcessorBuilder.from(core)
                        .mdc("traceId", s -> "t-" + s)
                        .logging()
                        .named("upper-log")
                        .build();

        assertTrue(composed.toString().startsWith("Mdc("));
        assertTrue(composed.toString().startsWith("Mdc([traceId], Logging(DEBUG, Catching("));
    }

    @Test
    public void caughtExceptionIsNotLogged() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            ThrowableFunction<String, String> transform =
                    input -> {
                        throw new IllegalStateException("boom");
                    };
            Processor<String, String> processor = ProcessorBuilder.from(transform).build();
            assertTrue(processor.process("x").isError());
        } finally {
            root.detachAppender(appender);
        }

        assertTrue(appender.list.isEmpty());
    }

    @Test
    public void loggingWithoutName() {
        Processor<String, String> composed =
                ProcessorBuilder.from(Processor.<String, String>of(s -> s)).logging().build();

        assertTrue(composed.toString().startsWith("Logging(DEBUG,"));
        assertTrue(composed.toString().startsWith("Logging(DEBUG, Catching("));
    }

    @Test
    public void processesWithMdc() {
        AtomicReference<String> seen = new AtomicReference<>();
        Processor<String, String> composed =
                ProcessorBuilder.from(
                                Processor.<String, String>of(
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
                ProcessorBuilder.from(
                                Processor.<String, String>of(
                                        s -> {
                                            order.add("main");
                                            return s.toUpperCase();
                                        }))
                        .before(Handler.of(s -> order.add("before")))
                        .after(
                                result -> {
                                    order.add("after");
                                    return done();
                                })
                        .mdc("k", s -> "v")
                        .build();

        assertEquals("X", composed.process("x").value());
        assertEquals(Arrays.asList("before", "main", "after"), order);
        assertTrue(composed.toString().contains("Around("));
        assertTrue(composed.toString().contains("Catching("));
        assertTrue(composed.toString().contains("Mdc([k],"));
        assertTrue(composed.toString().indexOf("Mdc(") < composed.toString().indexOf("Catching("));
        assertTrue(
                composed.toString().indexOf("Catching(") < composed.toString().indexOf("Around("));
    }

    @Test
    public void builderAcceptsFunctionAndBeforeAfterConsumers() {
        List<String> order = new ArrayList<>();
        Processor<String, String> composed =
                ProcessorBuilder.from(
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
        assertEquals(Arrays.asList("before", "main", "after"), order);
    }

    @Test
    public void rawProcessorThrowBecomesError() {
        Processor<String, String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        Processor<String, String> composed = ProcessorBuilder.from(raw).build();

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
                ProcessorBuilder.from(raw).onException(failure -> done("recovered")).build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
    }

    @Test
    public void builderFunctionStillMapsExceptionToError() {
        Processor<String, String> composed =
                ProcessorBuilder.from(
                                Processor.<String, String>of(
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
                ProcessorBuilder.from(transform)
                        .onException(
                                failure -> {
                                    assertInstanceOf(IOException.class, failure.exception());
                                    assertEquals("checked", failure.exception().getMessage());
                                    return done("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
    }

    @Test
    public void beforeHandlerErrorSkipsCoreAndReturnsError() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicBoolean afterCalled = new AtomicBoolean();
        AtomicBoolean onExceptionCalled = new AtomicBoolean();
        Processor<String, String> composed =
                ProcessorBuilder.from(
                                (String s) -> {
                                    coreCalled.set(true);
                                    return s;
                                })
                        .before(
                                Handler.<String>of(
                                        s -> {
                                            throw new IllegalStateException("hook");
                                        }))
                        .after(
                                result -> {
                                    afterCalled.set(true);
                                    return done();
                                })
                        .onException(
                                failure -> {
                                    onExceptionCalled.set(true);
                                    return done("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");

        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertEquals("hook", result.exception().getMessage());
        assertFalse(result.hasValue());
        assertFalse(coreCalled.get());
        assertFalse(afterCalled.get());
        assertFalse(onExceptionCalled.get());
    }

    @Test
    public void afterHandlerErrorReplacesCoreSuccess() {
        AtomicBoolean onExceptionCalled = new AtomicBoolean();
        Processor<String, String> composed =
                ProcessorBuilder.from((String s) -> "ok")
                        .after(
                                Handler.<Processed<String>>of(
                                        result -> {
                                            throw new IllegalStateException("hook");
                                        }))
                        .onException(
                                failure -> {
                                    onExceptionCalled.set(true);
                                    return done("recovered");
                                })
                        .build();

        Processed<String> result = composed.process("x");

        assertTrue(result.isError());
        assertEquals("hook", result.exception().getMessage());
        assertFalse(result.hasValue());
        assertFalse(onExceptionCalled.get());
    }

    @Test
    public void afterHandlerErrorWhenCoreReturnsErrorSuppressesCoreException() {
        IllegalStateException core = new IllegalStateException("core");
        IllegalStateException hook = new IllegalStateException("hook");
        Processor<String, String> coreProcessor = input -> error(core);
        Processor<String, String> composed =
                ProcessorBuilder.from(coreProcessor)
                        .after(
                                result -> {
                                    return Handled.error(hook);
                                })
                        .build();

        Processed<String> result = composed.process("x");

        assertTrue(result.isError());
        assertSame(hook, result.exception());
        assertArrayEquals(new Throwable[] {core}, result.exception().getSuppressed());
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
                ProcessorBuilder.from(
                                (String s) -> {
                                    coreCalled.set(true);
                                    return s;
                                })
                        .before(before)
                        .onException(
                                failure -> {
                                    seen.set(failure.exception());
                                    return done("recovered");
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
                ProcessorBuilder.from((String s) -> "ok")
                        .after(after)
                        .onException(
                                failure -> {
                                    seen.set(failure.exception());
                                    return done("recovered");
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
                ProcessorBuilder.from(
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
        assertEquals(Collections.emptyList(), order);
        assertTrue(composed.toString().contains("Catching(Conditional(Around("));
    }

    @Test
    public void whenTrueRunsBeforeCoreAndAfter() {
        List<String> order = new ArrayList<>();
        Processor<String, String> composed =
                ProcessorBuilder.from(
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
        assertEquals(Arrays.asList("before", "main", "after"), order);
    }

    @Test
    public void whenPredicateExceptionReachesOnException() {
        AtomicReference<Exception> seen = new AtomicReference<>();
        Processor<String, String> composed =
                ProcessorBuilder.from((String s) -> s)
                        .when(
                                s -> {
                                    throw new IOException("pred");
                                })
                        .onException(
                                failure -> {
                                    seen.set(failure.exception());
                                    return done("recovered");
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
                ProcessorBuilder.from((String s) -> s).when(s -> true).build();

        assertEquals("x", composed.process("x").value());
        assertTrue(composed.toString().contains("Catching(Conditional("));
        assertFalse(composed.toString().contains("Around("));
    }

    @Test
    public void secondWhenReplacesFirst() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        Processor<String, String> composed =
                ProcessorBuilder.from(
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
    public void builderFunctionNullIsDoneWithoutPayload() {
        ThrowableFunction<String, String> transform = s -> null;
        Processor<String, String> composed = ProcessorBuilder.from(transform).build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertFalse(result.isSkipped());
        assertFalse(result.hasValue());
    }

    @Test
    public void loggingUsesCoreClassNotCatchingWrapper() {
        Processor<String, Integer> composed =
                ProcessorBuilder.from(new ParseAmountProcessor()).named("amount").logging().build();

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

    private static final class ParseAmountProcessor extends FunctionProcessor<String, Integer> {

        private ParseAmountProcessor() {
            super(Integer::valueOf);
        }
    }
}
