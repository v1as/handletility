package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.skipped;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.ProcessorOutcome;

public class ProcessorAfterTest {

    @Test
    public void keepsMainResultIncludingNullPayload() {
        String input = "marko";
        AtomicReference<Processed<String>> mainResult =
                new AtomicReference<>(Processed.done("out"));
        AtomicReference<ProcessorOutcome<String, String>> seen = new AtomicReference<>();
        Processor<String, String> chain =
                ((Processor<String, String>) value -> mainResult.get())
                        .after(outcome -> seen.set(outcome));
        assertInstanceOf(AfterProcessor.class, chain);

        Processed<String> done = chain.process(input);
        assertSame(mainResult.get(), done);
        assertSame(input, seen.get().input());
        assertSame(mainResult.get(), seen.get().processed());

        Processed<String> empty = Processed.done(null);
        mainResult.set(empty);
        assertSame(empty, chain.process(input));
        assertTrue(chain.process(input).isDone());
        assertNull(chain.process(input).value());

        mainResult.set(skipped());
        assertSame(skipped(), chain.process(input));

        IllegalStateException boom = new IllegalStateException("boom");
        Processed<String> failed = Processed.error(boom);
        mainResult.set(failed);
        assertSame(failed, chain.process(input));
        assertSame(boom, seen.get().processed().exception());
    }

    @Test
    public void finisherExceptionReplacesMainAndSuppressesPreviousFailure() {
        IllegalStateException hook = new IllegalStateException("hook");
        Processed<String> replaced =
                ((Processor<String, String>) value -> Processed.done("out"))
                        .after(
                                outcome -> {
                                    throw hook;
                                })
                        .process("x");
        assertTrue(replaced.isError());
        assertSame(hook, replaced.exception());
        assertFalse(replaced.hasValue());
        assertEquals(0, hook.getSuppressed().length);

        IllegalStateException core = new IllegalStateException("core");
        Processed<String> both =
                ((Processor<String, String>) value -> Processed.error(core))
                        .after(
                                outcome -> {
                                    throw hook;
                                })
                        .process("x");
        assertSame(hook, both.exception());
        assertFalse(both.hasValue());
        assertArrayEquals(new Throwable[] {core}, hook.getSuppressed());

        Processed<String> same =
                ((Processor<String, String>) value -> Processed.error(core))
                        .after(
                                outcome -> {
                                    throw core;
                                })
                        .process("x");
        assertSame(core, same.exception());
        assertEquals(0, core.getSuppressed().length);
    }

    @Test
    public void checkedFinisherExceptionBecomesErrorAndMainThrowSkipsFinisher() {
        Processed<String> result =
                ((Processor<String, String>) value -> Processed.done("out"))
                        .after(
                                outcome -> {
                                    throw new IOException("audit");
                                })
                        .process("x");
        assertTrue(result.isError());
        assertInstanceOf(IOException.class, result.exception());
        assertFalse(result.hasValue());

        AtomicBoolean finisherCalled = new AtomicBoolean();
        Processor<String, String> throwing =
                value -> {
                    throw new IllegalStateException("escape");
                };
        assertThrows(
                IllegalStateException.class,
                () ->
                        throwing.after(
                                        outcome -> {
                                            finisherCalled.set(true);
                                        })
                                .process("x"));
        assertFalse(finisherCalled.get());

        assertThrows(
                Error.class,
                () ->
                        ((Processor<String, String>) value -> Processed.done("out"))
                                .after(
                                        outcome -> {
                                            throw new Error("boom");
                                        })
                                .process("x"));
    }

    @Test
    public void rejectsNullNext() {
        Processor<String, String> processor = value -> Processed.done("out");
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> processor.after(null));
        assertEquals("next cannot be null", ex.getMessage());
    }
}
