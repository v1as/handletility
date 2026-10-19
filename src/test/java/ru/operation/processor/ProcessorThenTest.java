package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;
import ru.operation.handler.Handlers;

public class ProcessorThenTest {

    @Test
    public void ofThenComposeThenApply() {
        Function<String, Integer> parse = Integer::parseInt;
        Function<Integer, Integer> twice = n -> n * 2;
        Function<Integer, String> toString = Object::toString;
        Processor<String, String> pipeline =
                Processors.of(parse).thenCompose(Processors.of(twice)).thenApply(toString);

        assertEquals("84", pipeline.process("42").value());
    }

    @Test
    public void thenComposePropagatesSkip() {
        Processor<String, Integer> skipping = input -> Processed.skipped();
        Function<Integer, String> toString = Object::toString;
        Processor<String, String> pipeline = skipping.thenApply(toString);

        assertTrue(pipeline.process("x").isSkipped());
    }

    @Test
    public void thenHandleDelegatesOnSuccess() {
        List<Integer> seen = new ArrayList<>();
        Function<String, Integer> parse = Integer::parseInt;
        Handler<String> handler = Processors.of(parse).thenHandle(Handlers.of(seen::add));

        assertTrue(handler.handle("7").isHandled());
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
        Function<String, Integer> parse = Integer::parseInt;
        Processor<String, Integer> even = Processors.of(parse).filter(n -> n % 2 == 0);

        assertEquals(42, even.process("42").value());
        assertTrue(even.process("41").isSkipped());
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
