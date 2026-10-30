package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;
import ru.operation.handler.Handlers;

public class ProcessorThenTest {

    @Test
    public void ofThenComposeThenApply() {
        Processor<String, String> pipeline =
                Processors.<String, Integer>of(Integer::parseInt)
                        .thenCompose(Processors.of((Integer n) -> n * 2))
                        .thenApply(Object::toString);

        assertEquals("84", pipeline.process("42").value());
    }

    @Test
    public void thenComposePropagatesSkip() {
        Processor<String, Integer> skipping = input -> Processed.skipped();
        Processor<String, String> pipeline = skipping.thenApply(Object::toString);

        assertTrue(pipeline.process("x").isSkipped());
    }

    @Test
    public void thenHandleDelegatesOnSuccess() {
        List<Integer> seen = new ArrayList<>();
        Handler<String> handler =
                Processors.<String, Integer>of(Integer::parseInt)
                        .thenHandle(Handlers.of(seen::add));

        assertTrue(handler.handle("7").isDone());
        assertEquals(List.of(7), seen);
    }

    @Test
    public void thenHandlePropagatesSkip() {
        List<Integer> seen = new ArrayList<>();
        Processor<String, Integer> skipping = input -> Processed.skipped();
        Handler<String> handler = skipping.thenHandle(Handlers.of(seen::add));

        assertTrue(handler.handle("x").isSkipped());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void filterKeepsMatchingOutput() {
        Processor<String, Integer> even =
                Processors.<String, Integer>of(Integer::parseInt).filter(n -> n % 2 == 0);

        assertEquals(42, even.process("42").value());
        assertTrue(even.process("41").isSkipped());
    }

    @Test
    public void orTriesNextOnlyOnSkip() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> first =
                input -> {
                    seen.add("first");
                    return Processed.skipped();
                };
        Processor<String, String> second =
                input -> {
                    seen.add("second");
                    return Processed.of(input + "-ok");
                };
        Processor<String, String> third =
                input -> {
                    seen.add("third");
                    return Processed.of("nope");
                };

        Processed<String> result = first.or(second).or(third).process("marko");

        assertEquals("marko-ok", result.value());
        assertEquals(List.of("first", "second"), seen);
    }

    @Test
    public void orStopsOnDoneAndError() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> next =
                input -> {
                    seen.add("next");
                    return Processed.of("other");
                };

        Processor<String, String> done = input -> Processed.of("kept");
        assertEquals("kept", done.or(next).process("x").value());
        assertTrue(seen.isEmpty());

        Processor<String, String> empty = input -> Processed.of(null);
        Processed<String> doneWithNull = empty.or(next).process("x");
        assertTrue(doneWithNull.isDone());
        assertNull(doneWithNull.value());
        assertTrue(seen.isEmpty());

        Processor<String, String> failing =
                input -> Processed.error(new IllegalStateException("boom"));
        Processed<String> error = failing.or(next).process("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void orStaysSkippedWhenEveryStepSkips() {
        Processor<String, String> skipping = input -> Processed.skipped();

        assertTrue(skipping.or(skipping).process("x").isSkipped());
    }

    @Test
    public void thenApplyTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> result =
                Processors.<String, Integer>of(s -> 1)
                        .<Integer>thenApply(
                                n -> {
                                    throw boom;
                                })
                        .process("x");

        assertTrue(result.isError());
        assertSame(boom, result.exception());
    }

    @Test
    public void filterTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> result =
                Processors.<String, Integer>of((String s) -> 1)
                        .filter(
                                n -> {
                                    throw boom;
                                })
                        .process("x");

        assertTrue(result.isError());
        assertSame(boom, result.exception());
    }

    @Test
    public void identifiedByBindsKeyAndDelegates() {
        IdentifiedProcessor<String, String, Integer> identified =
                Processors.<String, Integer>of(Integer::parseInt).identifiedBy("parse");

        assertEquals("parse", identified.getIdentifier());
        assertEquals(42, identified.process("42").value());
    }

    @Test
    public void filterPropagatesUpstreamSkipAndError() {
        Processor<String, Integer> skipping = input -> Processed.skipped();
        assertTrue(skipping.filter(n -> true).process("x").isSkipped());

        Processor<String, Integer> failing =
                input -> Processed.error(new IllegalStateException("boom"));
        Processed<Integer> error = failing.filter(n -> true).process("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
    }
}
