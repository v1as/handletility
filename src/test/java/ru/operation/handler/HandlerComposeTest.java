package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import ru.operation.handler.impl.IdentifiedByConstHandler;
import ru.operation.handler.impl.MdcHandler;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.handler.impl.wrap.LoggingHandler;

public class HandlerComposeTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void buildOrderIsIdentifiedLoggingMdcCore() {
        Handler<String> core = new SimpleHandler<>(s -> {});
        IdentifiedHandler<String, String> composed =
                Handlers.compose(core)
                        .identified("order-persist")
                        .mdc("requestId", s -> "id-" + s)
                        .named("persist")
                        .logging()
                        .build();

        assertInstanceOf(IdentifiedByConstHandler.class, composed);
        assertEquals("order-persist", composed.getIdentifier());
        assertTrue(
                composed.toString()
                        .startsWith(
                                "Identified[order-persist](Mdc([requestId], Logging(DEBUG,"
                                        + " Catching("));
    }

    @Test
    public void loggingUsesIdentifierWhenNameAbsent() {
        IdentifiedHandler<String, String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> {}))
                        .identified("only-id")
                        .logging(Level.TRACE)
                        .build();

        assertEquals("only-id", composed.getIdentifier());
        IdentifiedByConstHandler<String, String> wrap =
                (IdentifiedByConstHandler<String, String>) composed;
        assertInstanceOf(LoggingHandler.class, wrap.getHandler());
        assertTrue(composed.toString().startsWith("Identified[only-id](Logging(TRACE, Catching("));
    }

    @Test
    public void mdcAndIdentifiedWorkTogether() {
        AtomicReference<String> seen = new AtomicReference<>();
        IdentifiedHandler<String, String> composed =
                Handlers.compose(new SimpleHandler<String>(input -> seen.set(MDC.get("requestId"))))
                        .identified("op")
                        .mdc("requestId", s -> "req-" + s)
                        .build();

        assertTrue(composed.handle("7").isHandled());
        assertEquals("req-7", seen.get());
        assertEquals("op", composed.getIdentifier());
    }

    @Test
    public void identifiedIsRequired() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Handlers.compose(new SimpleHandler<String>(s -> {})).logging().build());
    }

    @Test
    public void namedRejectsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Handlers.compose(new SimpleHandler<String>(s -> {}))
                                .identified("id")
                                .named(null));
    }

    @Test
    public void mdcWithoutLoggingStillIdentified() {
        IdentifiedHandler<String, String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> {}))
                        .identified("k")
                        .mdc("k", s -> s)
                        .build();
        IdentifiedByConstHandler<String, String> wrap =
                (IdentifiedByConstHandler<String, String>) composed;
        assertInstanceOf(MdcHandler.class, wrap.getHandler());
    }

    @Test
    public void beforeAndAfterWrapCoreInsideMdc() {
        List<String> order = new ArrayList<>();
        IdentifiedHandler<String, String> composed =
                Handlers.compose(new SimpleHandler<String>(s -> order.add("main")))
                        .identified("op")
                        .before(new SimpleHandler<>(s -> order.add("before")))
                        .after(
                                result -> {
                                    order.add("after");
                                    return result;
                                })
                        .mdc("k", s -> "v")
                        .build();

        assertTrue(composed.handle("x").isHandled());
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
        IdentifiedHandler<String, String> composed =
                Handlers.compose(
                                (String s) -> {
                                    order.add("main");
                                })
                        .identified("op")
                        .before(
                                (String s) -> {
                                    order.add("before");
                                })
                        .after(
                                (Handled result) -> {
                                    order.add("after");
                                })
                        .build();

        assertTrue(composed.handle("x").isHandled());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void rawHandlerThrowBecomesError() {
        Handler<String> raw =
                input -> {
                    throw new IllegalStateException("boom");
                };
        IdentifiedHandler<String, String> composed =
                Handlers.compose(raw).identified("raw-throw").build();

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
        IdentifiedHandler<String, String> composed =
                Handlers.compose(raw)
                        .identified("recover")
                        .onException(failed -> Handled.ok())
                        .build();

        assertTrue(composed.handle("x").isHandled());
    }

    @Test
    public void composeConsumerStillMapsExceptionToError() {
        IdentifiedHandler<String, String> composed =
                Handlers.compose(
                                (String s) -> {
                                    throw new RuntimeException("from-consumer");
                                })
                        .identified("consumer-throw")
                        .build();

        Handled result = composed.handle("x");
        assertTrue(result.isError());
        assertEquals("from-consumer", result.exception().getMessage());
    }
}
