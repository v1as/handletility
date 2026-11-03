package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class HandlerAndTest {

    @Test
    public void andRunsNextOnlyOnDone() {
        List<String> seen = new ArrayList<>();
        String input = "marko";
        AtomicReference<String> received = new AtomicReference<>();
        Handler<String> first =
                value -> {
                    seen.add("first");
                    return done();
                };
        Handler<String> second =
                value -> {
                    received.set(value);
                    seen.add("second");
                    return done();
                };
        Handler<String> third =
                value -> {
                    seen.add("third");
                    return done();
                };

        assertTrue(first.and(second).and(third).handle(input).isDone());
        assertSame(input, received.get());
        assertEquals(List.of("first", "second", "third"), seen);

        seen.clear();
        Handler<String> skipping =
                value -> {
                    seen.add("skip");
                    return skipped();
                };
        assertTrue(first.and(skipping).and(third).handle(input).isSkipped());
        assertEquals(List.of("first", "skip"), seen);

        IllegalStateException boom = new IllegalStateException("boom");
        Handled error = first.and(value -> error(boom)).handle(input);
        assertTrue(error.isError());
        assertSame(boom, error.exception());
    }

    @Test
    public void andStopsOnSkipAndError() {
        List<String> seen = new ArrayList<>();
        Handler<String> next =
                value -> {
                    seen.add("next");
                    return done();
                };

        Handler<String> skipping = value -> skipped();
        assertTrue(skipping.and(next).handle("x").isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Handler<String> failing = value -> error(boom);
        Handled error = failing.and(next).handle("x");
        assertTrue(error.isError());
        assertSame(boom, error.exception());
        assertTrue(seen.isEmpty());
    }
}
