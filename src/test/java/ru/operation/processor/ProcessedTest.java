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
        assertSame(error.exception(), assertThrows(ProcessorException.class, error::throwIfError));
    }

    @Test
    public void throwIfErrorKeepsRuntimeAndCheckedMessage() {
        IllegalStateException runtime = new IllegalStateException("boom");
        assertSame(
                runtime,
                assertThrows(
                        IllegalStateException.class,
                        () -> Processed.error(runtime).throwIfError()));

        IOException checked = new IOException("disk");
        ProcessorException wrapped =
                assertThrows(
                        ProcessorException.class,
                        () -> Processed.<Integer>error(checked).throwIfError());
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
    }

    @Test
    public void doneNullPayloadChainsAndDiffersFromSkipped() {
        Processed<String> doneNull = Processed.of("x").map(s -> (String) null);
        assertTrue(doneNull.isDone());
        assertFalse(doneNull.isSkipped());
        assertFalse(doneNull.hasValue());

        AtomicInteger calls = new AtomicInteger();
        Processed<String> chained =
                doneNull.flatMap(
                        v -> {
                            calls.incrementAndGet();
                            return Processed.of("next");
                        });
        assertEquals(1, calls.get());
        assertEquals("next", chained.throwIfError());

        assertFalse(Processed.<String>skipped().flatMap(v -> Processed.of("nope")).isDone());
    }

    @Test
    public void simpleProcessorNullIsDone() {
        Processed<String> result = new SimpleProcessor<String, String>(s -> null).process("in");
        assertTrue(result.isDone());
        assertFalse(result.isSkipped());
        assertFalse(result.hasValue());
    }

    @Test
    public void mapTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> result =
                Processed.of(1)
                        .map(
                                n -> {
                                    throw boom;
                                });

        assertTrue(result.isError());
        assertSame(boom, result.exception());
    }

    @Test
    public void mapDoesNotCatchError() {
        assertThrows(
                AssertionError.class,
                () ->
                        Processed.of(1)
                                .map(
                                        n -> {
                                            throw new AssertionError("boom");
                                        }));
    }

    @Test
    public void filterSkipsOnFalseAndTurnsExceptionIntoError() {
        assertTrue(Processed.of(1).filter(n -> false).isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> error =
                Processed.of(1)
                        .filter(
                                n -> {
                                    throw boom;
                                });
        assertTrue(error.isError());
        assertSame(boom, error.exception());
    }

    @Test
    public void mapAndFlatMap() {
        assertEquals(4, Processed.of(2).map(n -> n * 2).value());
        assertFalse(Processed.<Integer>skipped().map(n -> n * 2).hasValue());

        Processed<Integer> chained =
                Processed.of("42").flatMap(new SimpleProcessor<>(Integer::valueOf));
        assertEquals(42, chained.throwIfError());

        assertTrue(
                Processed.<String>error("x")
                        .flatMap(new SimpleProcessor<>(Integer::valueOf))
                        .isError());
    }

    @Test
    public void thenHandleAndToOptional() {
        AtomicInteger sum = new AtomicInteger();
        Handled handled = Processed.of(3).thenHandle(new SimpleHandler<>(n -> sum.addAndGet(n)));
        assertTrue(handled.isDone());
        assertEquals(3, sum.get());

        assertTrue(Processed.<Integer>skipped().thenHandle(n -> Handled.ok()).isSkipped());
        assertEquals("a", Processed.of("a").toOptional().orElseThrow());
        assertTrue(Processed.<String>skipped().toOptional().isEmpty());
    }

    @Test
    public void constructorRejectsInvalidCombinations() {
        IllegalStateException boom = new IllegalStateException("boom");

        assertThrows(
                IllegalArgumentException.class,
                () -> new Processed<>("ok", ResultState.ERROR, boom));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Processed<>("ok", ResultState.SKIPPED, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Processed<>("ok", ResultState.DONE, boom));
        assertThrows(IllegalArgumentException.class, () -> Processed.error((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> new Processed<>(null, null, null));
    }
}
