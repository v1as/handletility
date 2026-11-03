package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class HandlerThenProcessTest {

    @Test
    public void thenProcessPassesTheSameInputWhenDone() {
        List<String> seen = new ArrayList<>();
        String input = "a1";
        AtomicReference<String> received = new AtomicReference<>();
        Processor<String, String> issue =
                Handler.of((String order) -> seen.add("save"))
                        .thenProcess(
                                Processor.of(
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
    public void thenProcessSkipsAndErrorsWithoutCallingNext() {
        List<String> seen = new ArrayList<>();
        Processor<String, String> next =
                Processor.of(
                        value -> {
                            seen.add(value);
                            return value;
                        });

        Handler<String> skipping = input -> skipped();
        assertTrue(skipping.thenProcess(next).process("x").isSkipped());

        IllegalStateException boom = new IllegalStateException("boom");
        Handler<String> failing = input -> error(boom);
        Processed<String> error = failing.thenProcess(next).process("x");

        assertTrue(error.isError());
        assertSame(boom, error.exception());
        assertTrue(seen.isEmpty());
    }
}
