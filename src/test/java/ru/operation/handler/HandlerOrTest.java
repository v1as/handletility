package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HandlerOrTest {

    @Test
    public void orTriesNextOnlyOnSkip() {
        List<String> seen = new ArrayList<>();
        Handler<String> first =
                input -> {
                    seen.add("first");
                    return skipped();
                };
        Handler<String> second =
                input -> {
                    seen.add("second");
                    return done();
                };
        Handler<String> third =
                input -> {
                    seen.add("third");
                    return done();
                };

        Handled result = first.or(second).or(third).handle("marko");

        assertTrue(result.isDone());
        assertEquals(List.of("first", "second"), seen);
    }

    @Test
    public void orStopsOnDoneAndError() {
        List<String> seen = new ArrayList<>();
        Handler<String> next =
                input -> {
                    seen.add("next");
                    return done();
                };

        Handler<String> done = input -> done();
        assertTrue(done.or(next).handle("x").isDone());
        assertTrue(seen.isEmpty());

        Handler<String> failing = input -> error(new IllegalStateException("boom"));
        Handled error = failing.or(next).handle("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void identifiedByBindsKeyAndDelegates() {
        List<String> seen = new ArrayList<>();
        IdentifiedHandler<String, String> identified =
                Handler.<String>of(seen::add).identifiedBy("audit");

        assertEquals("audit", identified.identifier());
        assertTrue(identified.handle("marko").isDone());
        assertEquals(List.of("marko"), seen);
    }

    @Test
    public void orStaysSkippedWhenEveryStepSkips() {
        Handler<String> skipping = input -> skipped();

        assertTrue(skipping.or(skipping).handle("x").isSkipped());
    }
}
