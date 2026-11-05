package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.util.function.ThrowableBiFunction;

public class ProcessorAndTest {

    @Test
    public void andCombinesPayloadsOfTheSameInput() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> first = input -> done(input + "-out");
        Processor<String, String> second =
                input -> {
                    seen.add(input);
                    return done(input);
                };

        Processed<String> result =
                first.and(second, (left, right) -> left + "|" + right).process("in");

        assertEquals("in-out|in", result.value());
        assertEquals(Arrays.asList("in"), seen);

        seen.clear();
        Processor<String, String> empty = input -> done(null);
        Processed<String> withNull =
                empty.and(second, (left, right) -> String.valueOf(left) + "|" + right)
                        .process("in");
        assertTrue(withNull.isDone());
        assertEquals("null|in", withNull.value());
        assertEquals(Arrays.asList("in"), seen);
    }

    @Test
    public void andStopsWhenEitherIsNotDone() {
        List<String> seen = new ArrayList<>();
        ThrowableBiFunction<String, String, String> combiner =
                (left, right) -> {
                    seen.add("combine");
                    return left + right;
                };
        Processor<String, String> next =
                input -> {
                    seen.add("next");
                    return done("right");
                };

        Processor<String, String> skipping = input -> skipped();
        assertTrue(skipping.and(next, combiner).process("x").isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Processor<String, String> failing = input -> error(boom);
        Processed<String> error = failing.and(next, combiner).process("x");
        assertTrue(error.isError());
        assertSame(boom, error.exception());
        assertTrue(seen.isEmpty());

        Processor<String, String> done = input -> done("left");
        Processor<String, String> nextSkip =
                input -> {
                    seen.add("next");
                    return skipped();
                };
        assertTrue(done.and(nextSkip, combiner).process("x").isSkipped());
        assertEquals(Arrays.asList("next"), seen);

        seen.clear();
        IllegalArgumentException nextBoom = new IllegalArgumentException("next");
        Processor<String, String> nextFail =
                input -> {
                    seen.add("next");
                    return error(nextBoom);
                };
        Processed<String> nextError = done.and(nextFail, combiner).process("x");
        assertTrue(nextError.isError());
        assertSame(nextBoom, nextError.exception());
        assertEquals(Arrays.asList("next"), seen);

        seen.clear();
        IllegalStateException combineBoom = new IllegalStateException("combine");
        ThrowableBiFunction<String, String, String> failingCombiner =
                (left, right) -> {
                    throw combineBoom;
                };
        Processed<String> combineError = done.and(next, failingCombiner).process("x");
        assertTrue(combineError.isError());
        assertSame(combineBoom, combineError.exception());
        assertEquals(Arrays.asList("next"), seen);
    }
}
