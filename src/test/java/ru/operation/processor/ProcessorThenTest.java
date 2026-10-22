package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Processor<String, Integer> even =
                Processors.<String, Integer>of(Integer::parseInt).filter(n -> n % 2 == 0);

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
