package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.slf4j.event.Level;
import ru.operation.handler.impl.MdcHandler;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.handler.impl.wrap.LoggingHandler;
import ru.operation.util.function.ThrowableConsumer;

public class HandlerComposeTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void buildOrderIsLoggingMdcCore() {
        Handler<String> core = new SimpleHandler<>(s -> {});
        Handler<String> composed =
                Handlers.compose(core)
                        .mdc("requestId", s -> "id-" + s)
                        .named("persist")
                        .logging()
                        .build();

        assertInstanceOf(MdcHandler.class, composed);
        assertTrue(composed.toString().startsWith("Mdc([requestId], Logging(DEBUG, Catching("));
    }

    @Test
    public void loggingWithoutName() {
        Handler<String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> {})).logging(Level.TRACE).build();

        assertInstanceOf(LoggingHandler.class, composed);
        assertTrue(composed.toString().startsWith("Logging(TRACE, Catching("));
    }

    @Test
    public void mdcReachesDelegate() {
        AtomicReference<String> seen = new AtomicReference<>();
        Handler<String> composed =
                Handlers.compose(new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))))
                        .mdc("requestId", s -> "req-" + s)
                        .build();

        assertTrue(composed.handle("7").isDone());
        assertEquals("req-7", seen.get());
    }

    @Test
    public void namedRejectsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Handlers.compose(new SimpleHandler<String>(s -> {})).named(null));
    }

    @Test
    public void mdcWithoutLogging() {
        Handler<String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> {})).mdc("k", s -> s).build();
        assertInstanceOf(MdcHandler.class, composed);
    }

    @Test
    public void beforeAndAfterWrapCoreInsideMdc() {
        List<String> order = new ArrayList<>();
        Handler<String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> order.add("main")))
                        .before(new SimpleHandler<>(s -> order.add("before")))
                        .after(
                                result -> {
                                    order.add("after");
                                    return result;
                                })
                        .mdc("k", s -> "v")
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertEquals(List.of("before", "main", "after"), order);
        assertTrue(composed.toString().contains("Around("));
        assertTrue(composed.toString().contains("Catching("));
        assertTrue(composed.toString().contains("Mdc([k],"));
        // Mdc wraps Catching(Around(...)) when no logging
        assertTrue(composed.toString().indexOf("Mdc(") < composed.toString().indexOf("Catching("));
        assertTrue(
                composed.toString().indexOf("Catching(") < composed.toString().indexOf("Around("));
    }

    @Test
    public void composeAcceptsConsumerAndBeforeAfterConsumers() {
        List<String> order = new ArrayList<>();
        Handler<String> composed =
                Handlers.compose(
                                (String s) -> {
                                    order.add("main");
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Handled result) -> {
                                    order.add("after");
                                })
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void rawHandlerThrowBecomesError() {
        Handler<String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        Handler<String> composed = Handlers.compose(raw).build();

        Handled result = composed.handle("x");
        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertEquals("boom", result.exception().getMessage());
    }

    @Test
    public void onExceptionCanRecover() {
        Handler<String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        Handler<String> composed =
                Handlers.compose(raw).onException(failed -> Handled.ok()).build();

        assertTrue(composed.handle("x").isDone());
    }

    @Test
    public void composeConsumerStillMapsExceptionToError() {
        Handler<String> composed =
                Handlers.compose(
                                (String s) -> {
                                    throw new RuntimeException("from-consumer");
                                })
                        .build();

        Handled result = composed.handle("x");
        assertTrue(result.isError());
        assertEquals("from-consumer", result.exception().getMessage());
    }

    @Test
    public void consumerOnExceptionReceivesOriginalCheckedException() {
        ThrowableConsumer<String> consumer =
                s -> {
                    throw new IOException("checked");
                };
        Handler<String> composed =
                Handlers.compose(consumer)
                        .onException(
                                failed -> {
                                    assertInstanceOf(IOException.class, failed.exception());
                                    assertEquals("checked", failed.exception().getMessage());
                                    return Handled.ok();
                                })
                        .build();

        assertTrue(composed.handle("x").isDone());
    }

    @Test
    public void beforeHandlerErrorSkipsCoreAndReturnsError() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicBoolean afterCalled = new AtomicBoolean();
        AtomicBoolean onExceptionCalled = new AtomicBoolean();
        Handler<String> composed =
                Handlers.compose((String s) -> coreCalled.set(true))
                        .before(
                                new SimpleHandler<String>(
                                        s -> {
                                            throw new IllegalStateException("hook");
                                        }))
                        .after(
                                result -> {
                                    afterCalled.set(true);
                                    return Handled.ok();
                                })
                        .onException(
                                failed -> {
                                    onExceptionCalled.set(true);
                                    return Handled.ok();
                                })
                        .build();

        Handled result = composed.handle("x");

        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertEquals("hook", result.exception().getMessage());
        assertFalse(coreCalled.get());
        assertFalse(afterCalled.get());
        assertFalse(onExceptionCalled.get());
    }

    @Test
    public void afterHandlerErrorReplacesCoreSuccess() {
        AtomicBoolean onExceptionCalled = new AtomicBoolean();
        Handler<String> composed =
                Handlers.compose((String s) -> {})
                        .after(
                                new SimpleHandler<Handled>(
                                        result -> {
                                            throw new IllegalStateException("hook");
                                        }))
                        .onException(
                                failed -> {
                                    onExceptionCalled.set(true);
                                    return Handled.ok();
                                })
                        .build();

        Handled result = composed.handle("x");

        assertTrue(result.isError());
        assertEquals("hook", result.exception().getMessage());
        assertFalse(onExceptionCalled.get());
    }

    @Test
    public void afterHandlerErrorWhenCoreFailedSuppressesCoreException() {
        IllegalStateException core = new IllegalStateException("core");
        IllegalStateException hook = new IllegalStateException("hook");
        Handler<String> coreHandler = input -> Handled.error(core);
        Handler<String> composed =
                Handlers.compose(coreHandler)
                        .after(
                                result -> {
                                    return Handled.error(hook);
                                })
                        .build();

        Handled result = composed.handle("x");

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
        Handler<String> composed =
                Handlers.compose((String s) -> coreCalled.set(true))
                        .before(before)
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Handled.ok();
                                })
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertFalse(coreCalled.get());
        assertInstanceOf(IllegalStateException.class, seen.get());
        assertEquals("hook", seen.get().getMessage());
    }

    @Test
    public void afterConsumerThrowReplacesCoreResult() {
        AtomicReference<Exception> seen = new AtomicReference<>();
        ThrowableConsumer<Handled> after =
                result -> {
                    throw new IllegalStateException("after");
                };
        Handler<String> composed =
                Handlers.compose((String s) -> {})
                        .after(after)
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Handled.error(failed.exception());
                                })
                        .build();

        Handled result = composed.handle("x");
        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertEquals("after", result.exception().getMessage());
        assertEquals("after", seen.get().getMessage());
    }

    @Test
    public void whenFalseSkipsBeforeCoreAndAfter() {
        List<String> order = new ArrayList<>();
        Handler<String> composed =
                Handlers.compose(
                                (String s) -> {
                                    order.add("main");
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Handled result) -> {
                                    order.add("after");
                                })
                        .when(s -> false)
                        .build();

        assertTrue(composed.handle("x").isSkipped());
        assertEquals(List.of(), order);
        assertTrue(composed.toString().contains("Catching(When(Around("));
    }

    @Test
    public void whenTrueRunsBeforeCoreAndAfter() {
        List<String> order = new ArrayList<>();
        Handler<String> composed =
                Handlers.compose(
                                (String s) -> {
                                    order.add("main");
                                })
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Handled result) -> {
                                    order.add("after");
                                })
                        .when(s -> "x".equals(s))
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void whenPredicateExceptionReachesOnException() {
        AtomicReference<Exception> seen = new AtomicReference<>();
        Handler<String> composed =
                Handlers.compose((String s) -> {})
                        .when(
                                s -> {
                                    throw new IOException("pred");
                                })
                        .onException(
                                failed -> {
                                    seen.set(failed.exception());
                                    return Handled.ok();
                                })
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertInstanceOf(IOException.class, seen.get());
        assertEquals("pred", seen.get().getMessage());
    }

    @Test
    public void whenWithoutAroundStillWrapsCore() {
        Handler<String> composed = Handlers.compose((String s) -> {}).when(s -> true).build();

        assertTrue(composed.handle("x").isDone());
        assertTrue(composed.toString().contains("Catching(When("));
        assertFalse(composed.toString().contains("Around("));
    }

    @Test
    public void secondWhenReplacesFirst() {
        List<String> order = new ArrayList<>();
        Handler<String> composed =
                Handlers.compose(
                                (String s) -> {
                                    order.add("main");
                                })
                        .when(s -> false)
                        .when(s -> true)
                        .build();

        assertTrue(composed.handle("x").isDone());
        assertEquals(List.of("main"), order);
    }

    @Test
    public void loggingUsesCoreClassNotCatchingWrapper() {
        Handler<String> composed =
                Handlers.compose(new CreateNoteCommandHandler()).named("note").logging().build();

        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            assertTrue(composed.handle("x").isDone());
        } finally {
            root.detachAppender(appender);
        }

        assertEquals(
                CreateNoteCommandHandler.class.getName() + "[note]",
                appender.list.stream()
                        .filter(event -> event.getFormattedMessage().startsWith("Handled input"))
                        .map(ILoggingEvent::getLoggerName)
                        .findFirst()
                        .orElseThrow());
    }

    private static final class CreateNoteCommandHandler implements Handler<String> {

        @Override
        public Handled handle(String input) {
            return Handled.ok();
        }
    }
}
