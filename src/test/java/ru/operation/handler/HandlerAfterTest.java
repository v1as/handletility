package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.HandlerOutcome;

public class HandlerAfterTest {

    @Test
    public void keepsMainResultAndPassesInputWithIt() {
        String input = "marko";
        AtomicReference<Handled> mainResult = new AtomicReference<>(done());
        AtomicReference<HandlerOutcome<String>> seen = new AtomicReference<>();
        Handler<String> chain =
                ((Handler<String>) value -> mainResult.get()).after(outcome -> seen.set(outcome));
        assertInstanceOf(AfterHandler.class, chain);

        assertSame(done(), chain.handle(input));
        assertSame(input, seen.get().input());
        assertSame(done(), seen.get().handled());

        mainResult.set(skipped());
        assertSame(skipped(), chain.handle(input));
        assertSame(skipped(), seen.get().handled());

        Handled failed = error(new IllegalStateException("boom"));
        mainResult.set(failed);
        assertSame(failed, chain.handle(input));
        assertSame(failed, seen.get().handled());
    }

    @Test
    public void finisherExceptionReplacesMainAndSuppressesPreviousFailure() {
        IllegalStateException hook = new IllegalStateException("hook");
        Handled replaced =
                ((Handler<String>) value -> done())
                        .after(
                                outcome -> {
                                    throw hook;
                                })
                        .handle("x");
        assertSame(hook, replaced.exception());
        assertEquals(0, hook.getSuppressed().length);

        IllegalStateException core = new IllegalStateException("core");
        Handled both =
                ((Handler<String>) value -> error(core))
                        .after(
                                outcome -> {
                                    throw hook;
                                })
                        .handle("x");
        assertSame(hook, both.exception());
        assertArrayEquals(new Throwable[] {core}, hook.getSuppressed());

        Handled same =
                ((Handler<String>) value -> error(core))
                        .after(
                                outcome -> {
                                    throw core;
                                })
                        .handle("x");
        assertSame(core, same.exception());
        assertEquals(0, core.getSuppressed().length);
    }

    @Test
    public void checkedFinisherExceptionBecomesErrorAndMainThrowSkipsFinisher() {
        Handled result =
                ((Handler<String>) value -> done())
                        .after(
                                outcome -> {
                                    throw new IOException("audit");
                                })
                        .handle("x");
        assertTrue(result.isError());
        assertInstanceOf(IOException.class, result.exception());

        AtomicBoolean finisherCalled = new AtomicBoolean();
        Handler<String> throwing =
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
                                .handle("x"));
        assertFalse(finisherCalled.get());

        assertThrows(
                Error.class,
                () ->
                        ((Handler<String>) value -> done())
                                .after(
                                        outcome -> {
                                            throw new Error("boom");
                                        })
                                .handle("x"));
    }

    @Test
    public void nestedAfterSeesLeftResultUntilAFinisherFails() {
        List<String> order = new ArrayList<>();
        AtomicReference<Handled> seenByOuter = new AtomicReference<>();
        Handler<String> main =
                value -> {
                    order.add("main");
                    return done();
                };
        Handler<String> chain =
                main.after(outcome -> order.add("inner"))
                        .after(
                                outcome -> {
                                    order.add("outer");
                                    seenByOuter.set(outcome.handled());
                                });

        assertSame(done(), chain.handle("x"));
        assertEquals(Arrays.asList("main", "inner", "outer"), order);
        assertSame(done(), seenByOuter.get());

        IllegalStateException hook = new IllegalStateException("hook");
        AtomicReference<Handled> seenAfterFailure = new AtomicReference<>();
        Handler<String> failed =
                ((Handler<String>) value -> done())
                        .after(
                                outcome -> {
                                    throw hook;
                                })
                        .after(outcome -> seenAfterFailure.set(outcome.handled()));
        Handled result = failed.handle("x");
        assertSame(hook, result.exception());
        assertSame(result, seenAfterFailure.get());
    }

    @Test
    public void skippedMainStillRunsAfterAndDoesNotContinueAnd() {
        List<String> seen = new ArrayList<>();
        Handler<String> next =
                value -> {
                    seen.add("and");
                    return done();
                };
        Handler<String> chain =
                ((Handler<String>)
                                value -> {
                                    seen.add("main");
                                    return skipped();
                                })
                        .after(outcome -> seen.add("after"))
                        .and(next);

        assertTrue(chain.handle("x").isSkipped());
        assertEquals(Arrays.asList("main", "after"), seen);
    }

    @Test
    public void rejectsNullNext() {
        Handler<String> handler = value -> done();
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> handler.after(null));
        assertEquals("next cannot be null", ex.getMessage());
    }
}
