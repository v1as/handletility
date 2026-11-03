package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;

public class ProcessorThenTest {

    @Test
    public void ofThenComposeThenApply() {
        Processor<String, String> pipeline =
                Processor.<String, Integer>of(Integer::parseInt)
                        .thenPipe(Processor.of((Integer n) -> n * 2))
                        .thenApply(Object::toString);

        assertEquals("84", pipeline.process("42").value());
    }

    @Test
    public void thenPipePropagatesSkip() {
        Processor<String, Integer> skipping = input -> skipped();
        Processor<String, String> pipeline = skipping.thenApply(Object::toString);

        assertTrue(pipeline.process("x").isSkipped());
    }

    @Test
    public void thenHandleDelegatesOnSuccess() {
        List<Integer> seen = new ArrayList<>();
        Handler<String> handler =
                Processor.<String, Integer>of(Integer::parseInt).thenHandle(Handler.of(seen::add));

        assertTrue(handler.handle("7").isDone());
        assertEquals(List.of(7), seen);
    }

    @Test
    public void thenHandlePropagatesSkip() {
        List<Integer> seen = new ArrayList<>();
        Processor<String, Integer> skipping = input -> skipped();
        Handler<String> handler = skipping.thenHandle(Handler.of(seen::add));

        assertTrue(handler.handle("x").isSkipped());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void filterKeepsMatchingOutput() {
        Processor<String, Integer> even =
                Processor.<String, Integer>of(Integer::parseInt).thenFilter(n -> n % 2 == 0);

        assertEquals(42, even.process("42").value());
        assertTrue(even.process("41").isSkipped());
    }

    @Test
    public void filterStartsChainWithTheSameInput() {
        List<String> seen = new ArrayList<>();
        String input = "ab";
        Processor<String, String> admitted = Processor.<String>when(value -> value.startsWith("a"));
        Processor<String, String> pipeline =
                admitted.thenApply(
                        value -> {
                            seen.add(value);
                            return value.toUpperCase();
                        });

        assertSame(input, admitted.process(input).value());
        assertEquals("AB", pipeline.process(input).value());
        assertEquals(List.of(input), seen);

        assertTrue(pipeline.process("b").isSkipped());
        assertEquals(List.of(input), seen);

        Processed<String> acceptedNull = Processor.<String>when(value -> true).process(null);
        assertTrue(acceptedNull.isDone());
        assertNull(acceptedNull.value());
    }

    @Test
    public void filterOnInputTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        List<String> seen = new ArrayList<>();
        Processed<String> result =
                Processor.<String>when(
                                value -> {
                                    throw boom;
                                })
                        .thenApply(
                                value -> {
                                    seen.add(value);
                                    return value;
                                })
                        .process("x");

        assertTrue(result.isError());
        assertSame(boom, result.exception());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void orTriesNextOnlyOnSkip() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> first =
                input -> {
                    seen.add("first");
                    return skipped();
                };
        Processor<String, String> second =
                input -> {
                    seen.add("second");
                    return done(input + "-ok");
                };
        Processor<String, String> third =
                input -> {
                    seen.add("third");
                    return done("nope");
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
                    return done("other");
                };

        Processor<String, String> done = input -> done("kept");
        assertEquals("kept", done.or(next).process("x").value());
        assertTrue(seen.isEmpty());

        Processor<String, String> empty = input -> done(null);
        Processed<String> doneWithNull = empty.or(next).process("x");
        assertTrue(doneWithNull.isDone());
        assertNull(doneWithNull.value());
        assertTrue(seen.isEmpty());

        Processor<String, String> failing = input -> error(new IllegalStateException("boom"));
        Processed<String> error = failing.or(next).process("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void orStaysSkippedWhenEveryStepSkips() {
        Processor<String, String> skipping = input -> skipped();

        assertTrue(skipping.or(skipping).process("x").isSkipped());
    }

    @Test
    public void thenApplyTurnsExceptionIntoError() {
        IllegalStateException boom = new IllegalStateException("boom");
        Processed<Integer> result =
                Processor.<String, Integer>of(s -> 1)
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
                Processor.<String, Integer>of((String s) -> 1)
                        .thenFilter(
                                n -> {
                                    throw boom;
                                })
                        .process("x");

        assertTrue(result.isError());
        assertSame(boom, result.exception());
    }

    @Test
    public void identifiedByBindsIdentifierAndDelegates() {
        IdentifiedProcessor<String, String, Integer> identified =
                Processor.<String, Integer>of(Integer::parseInt).identifiedBy("parse");

        assertEquals("parse", identified.identifier());
        assertEquals(42, identified.process("42").value());
    }

    @Test
    public void filterPropagatesUpstreamSkipAndError() {
        Processor<String, Integer> skipping = input -> skipped();
        assertTrue(skipping.thenFilter(n -> true).process("x").isSkipped());

        Processor<String, Integer> failing = input -> error(new IllegalStateException("boom"));
        Processed<Integer> error = failing.thenFilter(n -> true).process("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
    }
}
