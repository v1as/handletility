package ru.operation.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;
import static ru.operation.processor.Processed.of;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.handler.impl.SimpleHandler;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class AroundProcessorTest {

    @Test
    public void afterReceivesMainResultAndHooksAreHandlers() {
        List<String> order = new ArrayList<>();
        AtomicReference<Processed<String>> seenAfter = new AtomicReference<>();
        Processor<String, String> around =
                AroundProcessor.<String, String>builder()
                        .beforeHandler(new SimpleHandler<>(s -> order.add("before")))
                        .processor(
                                new SimpleProcessor<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(
                                result -> {
                                    order.add("after");
                                    seenAfter.set(result);
                                    return ok();
                                })
                        .build();

        Processed<String> result = around.process("x");

        assertTrue(result.isError());
        assertSame(result, seenAfter.get());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void returnsMainProcessorResult() {
        Processor<String, Integer> around =
                AroundProcessor.<String, Integer>builder()
                        .processor(new SimpleProcessor<>(String::length))
                        .build();

        assertEquals(of(3), around.process("abc"));
    }
}
