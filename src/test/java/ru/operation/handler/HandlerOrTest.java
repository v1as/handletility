package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                    return Handled.skipped();
                };
        Handler<String> second =
                input -> {
                    seen.add("second");
                    return Handled.ok();
                };
        Handler<String> third =
                input -> {
                    seen.add("third");
                    return Handled.ok();
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
                    return Handled.ok();
                };

        Handler<String> done = input -> Handled.ok();
        assertTrue(done.or(next).handle("x").isDone());
        assertTrue(seen.isEmpty());

        Handler<String> failing = input -> Handled.error(new IllegalStateException("boom"));
        Handled error = failing.or(next).handle("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void orStaysSkippedWhenEveryStepSkips() {
        Handler<String> skipping = input -> Handled.skipped();

        assertTrue(skipping.or(skipping).handle("x").isSkipped());
    }
}
