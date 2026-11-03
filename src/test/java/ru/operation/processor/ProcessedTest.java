package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.operation.ResultState;
import ru.operation.handler.Handled;
import ru.operation.handler.impl.ConsumerHandler;
import ru.operation.processor.impl.FunctionProcessor;

public class ProcessedTest {

    @Test
    public void stateHelpers() {
        assertTrue(done(1).isDone());
        assertTrue(done(1).hasValue());
        assertFalse(done(1).isError());
        assertFalse(done(1).isSkipped());

        assertTrue(skipped().isSkipped());
        assertFalse(skipped().hasValue());

        Processed<Integer> error = error("fail");
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
                assertThrows(IllegalStateException.class, () -> error(runtime).throwIfError()));

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
        Processed<String> doneNull = done("x").map(s -> (String) null);
        assertTrue(doneNull.isDone());
        assertFalse(doneNull.isSkipped());
        assertFalse(doneNull.hasValue());

        AtomicInteger calls = new AtomicInteger();
        Processed<String> chained =
                doneNull.flatMap(
                        v -> {
                            calls.incrementAndGet();
                            return done("next");
                        });
        assertEquals(1, calls.get());
        assertEquals("next", chained.requireDone());

        assertFalse(Processed.<String>skipped().flatMap(v -> done("nope")).isDone());
    }

    @Test
    public void simpleProcessorNullIsDone() {
        Processed<String> result = new FunctionProcessor<String, String>(s -> null).process("in");
        assertTrue(result.isDone());
        assertFalse(result.isSkipped());
        assertFalse(result.hasValue());
    }

    @Test
    public void mapTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> result =
                done(1).map(
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
                        done(1).map(
                                        n -> {
                                            throw new AssertionError("boom");
                                        }));
    }

    @Test
    public void filterSkipsOnFalseAndTurnsExceptionIntoError() {
        assertTrue(done(1).filter(n -> false).isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> error =
                done(1).filter(
                                n -> {
                                    throw boom;
                                });
        assertTrue(error.isError());
        assertSame(boom, error.exception());
    }

    @Test
    public void mapAndFlatMap() {
        assertEquals(4, done(2).map(n -> n * 2).value());
        assertFalse(Processed.<Integer>skipped().map(n -> n * 2).hasValue());

        Processed<Integer> chained = done("42").flatMap(new FunctionProcessor<>(Integer::valueOf));
        assertEquals(42, chained.requireDone());

        assertTrue(
                Processed.<String>error("x")
                        .flatMap(new FunctionProcessor<>(Integer::valueOf))
                        .isError());
    }

    @Test
    public void handle() {
        AtomicInteger sum = new AtomicInteger();
        Handled handled = done(3).handle(new ConsumerHandler<>(n -> sum.addAndGet(n)));
        assertTrue(handled.isDone());
        assertEquals(3, sum.get());

        assertTrue(Processed.<Integer>skipped().handle(n -> Handled.done()).isSkipped());
    }

    @Test
    public void throwIfErrorPassesThroughDoneAndSkipped() {
        Processed<String> done = done("a");
        assertSame(done, done.throwIfError());

        Processed<String> doneNull = done((String) null);
        assertSame(doneNull, doneNull.throwIfError());
        assertNull(doneNull.value());

        assertSame(Processed.<String>skipped(), Processed.<String>skipped().throwIfError());
    }

    @Test
    public void requireDoneReturnsPayloadAndRejectsSkip() {
        assertEquals("a", done("a").requireDone());
        assertNull(done((String) null).requireDone());

        ProcessorException skipped =
                assertThrows(
                        ProcessorException.class, () -> Processed.<String>skipped().requireDone());
        assertNull(skipped.getCause());

        Processed<String> error = error("disk");
        assertSame(error.exception(), assertThrows(ProcessorException.class, error::requireDone));
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
        assertThrows(IllegalArgumentException.class, () -> error((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> new Processed<>(null, null, null));
    }
}
