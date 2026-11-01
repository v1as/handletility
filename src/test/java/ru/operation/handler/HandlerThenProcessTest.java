package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.Processors;

public class HandlerThenProcessTest {

    @Test
    public void thenProcessPassesTheSameInputWhenDone() {
        List<String> seen = new ArrayList<>();
        String input = "a1";
        AtomicReference<String> received = new AtomicReference<>();
        Processor<String, String> issue =
                Handlers.of((String order) -> seen.add("save"))
                        .thenProcess(
                                Processors.of(
                                        order -> {
                                            received.set(order);
                                            seen.add("issue");
                                            return order + "-receipt";
                                        }));

        Processed<String> result = issue.process(input);

        assertEquals("a1-receipt", result.value());
        assertSame(input, received.get());
        assertEquals(List.of("save", "issue"), seen);
    }

    @Test
    public void thenProcessPropagatesSkipAndError() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> next =
                Processors.of(
                        value -> {
                            seen.add(value);
                            return value;
                        });

        Handler<String> skipping = input -> Handled.skipped();
        assertTrue(skipping.thenProcess(next).process("x").isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Handler<String> failing = input -> Handled.error(boom);
        Processed<String> error = failing.thenProcess(next).process("x");

        assertTrue(error.isError());
        assertSame(boom, error.exception());
        assertTrue(seen.isEmpty());
    }
}
