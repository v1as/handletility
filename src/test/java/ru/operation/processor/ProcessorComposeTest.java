package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.processor.impl.IdentifiedByConstProcessor;
import ru.operation.processor.impl.SimpleProcessor;
import ru.operation.processor.impl.wrap.LoggingProcessor;

public class ProcessorComposeTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    public void buildOrderIsIdentifiedLoggingMdcCore() {
        Processor<String, String> core = new SimpleProcessor<>(String::toUpperCase);
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(core)
                        .identified("upper")
                        .mdc("traceId", s -> "t-" + s)
                        .logging()
                        .named("upper-log")
                        .build();

        assertInstanceOf(IdentifiedByConstProcessor.class, composed);
        assertEquals("upper", composed.getIdentifier());
        assertTrue(
                composed.toString()
                        .startsWith("Identified[upper](Mdc([traceId], Logging(DEBUG, Catching("));
    }

    @Test
    public void loggingUsesIdWhenNameAbsent() {
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(new SimpleProcessor<String, String>(s -> s))
                        .identified("pid")
                        .logging()
                        .build();

        IdentifiedByConstProcessor<String, String, String> wrap =
                (IdentifiedByConstProcessor<String, String, String>) composed;
        assertInstanceOf(LoggingProcessor.class, wrap.getProcessor());
        assertTrue(composed.toString().startsWith("Identified[pid](Logging(DEBUG, Catching("));
    }

    @Test
    public void processesWithMdc() {
        AtomicReference<String> seen = new AtomicReference<>();
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(
                                new SimpleProcessor<String, String>(
                                        input -> {
                                            seen.set(MDC.get("traceId"));
                                            return input;
                                        }))
                        .identified("p")
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
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(
                                new SimpleProcessor<String, String>(
                                        s -> {
                                            order.add("main");
                                            return s.toUpperCase();
                                        }))
                        .identified("op")
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
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(
                                (String s) -> {
                                    order.add("main");
                                    return s.toUpperCase();
                                })
                        .identified("op")
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
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(raw).identified("raw-throw").build();

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
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(raw)
                        .identified("recover")
                        .onException(failed -> Processed.of("recovered"))
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isDone());
        assertEquals("recovered", result.value());
    }

    @Test
    public void composeFunctionStillMapsExceptionToError() {
        IdentifiedProcessor<String, String, String> composed =
                Processors.compose(
                                Processors.<String, String>of(
                                        s -> {
                                            throw new RuntimeException("from-function");
                                        }))
                        .identified("function-throw")
                        .build();

        Processed<String> result = composed.process("x");
        assertTrue(result.isError());
        assertEquals("from-function", result.exception().getMessage());
    }
}
