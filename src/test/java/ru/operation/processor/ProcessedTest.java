package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.operation.ResultState;
import ru.operation.handler.Handled;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.processor.impl.SimpleProcessor;

public class ProcessedTest {

    @Test
    public void stateHelpers() {
        assertTrue(Processed.of(1).isDone());
        assertTrue(Processed.of(1).hasValue());
        assertFalse(Processed.of(1).isError());
        assertFalse(Processed.of(1).isSkipped());

        assertTrue(Processed.skipped().isSkipped());
        assertFalse(Processed.skipped().hasValue());

        Processed<Integer> error = Processed.error("fail");
        assertTrue(error.isError());
        assertFalse(error.hasValue());
        assertEquals(ResultState.ERROR, error.state());
        assertSame(error.exception(), assertThrows(ProcessorException.class, error::orElseThrow));
    }

    @Test
    public void orElseThrowKeepsRuntimeAndCheckedMessage() {
        IllegalStateException runtime = new IllegalStateException("boom");
        assertSame(
                runtime,
                assertThrows(
                        IllegalStateException.class, () -> Processed.error(runtime).orElseThrow()));

        IOException checked = new IOException("disk");
        ProcessorException wrapped =
                assertThrows(
                        ProcessorException.class,
                        () -> Processed.<Integer>error(checked).orElseThrow());
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
    }

    @Test
    public void mapAndFlatMap() {
        assertEquals(4, Processed.of(2).map(n -> n * 2).value());
        assertFalse(Processed.<Integer>skipped().map(n -> n * 2).hasValue());

        Processed<Integer> chained =
                Processed.of("42").flatMap(new SimpleProcessor<>(Integer::valueOf));
        assertEquals(42, chained.orElseThrow());

        assertTrue(
                Processed.<String>error("x")
                        .flatMap(new SimpleProcessor<>(Integer::valueOf))
                        .isError());
    }

    @Test
    public void thenHandleAndToOptional() {
        AtomicInteger sum = new AtomicInteger();
        Handled handled = Processed.of(3).thenHandle(new SimpleHandler<>(n -> sum.addAndGet(n)));
        assertTrue(handled.isHandled());
        assertEquals(3, sum.get());

        assertTrue(Processed.<Integer>skipped().thenHandle(n -> Handled.ok()).isSkipped());
        assertEquals("a", Processed.of("a").toOptional().orElseThrow());
        assertTrue(Processed.<String>skipped().toOptional().isEmpty());
    }
}
