package ru.operation.processor.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.processor.Processed.of;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
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
                        .delegate(
                                new SimpleProcessor<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(
                                result -> {
                                    order.add("after");
                                    seenAfter.set(result);
                                    return done();
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
                        .delegate(new SimpleProcessor<>(String::length))
                        .build();

        assertEquals(of(3), around.process("abc"));
    }

    @Test
    public void skippedAndDoneHooksDoNotReplaceCore() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        Processor<String, String> skippedBefore =
                AroundProcessor.<String, String>builder()
                        .beforeHandler(input -> Handled.skipped())
                        .delegate(
                                input -> {
                                    coreCalled.set(true);
                                    return Processed.error(new IllegalStateException("core"));
                                })
                        .build();

        Processed<String> skippedBeforeResult = skippedBefore.process("x");

        assertTrue(coreCalled.get());
        assertTrue(skippedBeforeResult.isError());
        assertEquals("core", skippedBeforeResult.exception().getMessage());

        Processor<String, String> doneAfter =
                AroundProcessor.<String, String>builder()
                        .delegate(input -> Processed.skipped())
                        .afterHandler(result -> done())
                        .build();

        assertTrue(doneAfter.process("x").isSkipped());
    }

    @Test
    public void beforeErrorSkipsCoreAndAfter() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicBoolean afterCalled = new AtomicBoolean();
        IOException hook = new IOException("hook");
        Processor<String, String> around =
                AroundProcessor.<String, String>builder()
                        .beforeHandler(input -> Handled.error(hook))
                        .delegate(
                                input -> {
                                    coreCalled.set(true);
                                    return of("ok");
                                })
                        .afterHandler(
                                result -> {
                                    afterCalled.set(true);
                                    return done();
                                })
                        .build();

        Processed<String> result = around.process("x");

        assertTrue(result.isError());
        assertSame(hook, result.exception());
        assertFalse(coreCalled.get());
        assertFalse(afterCalled.get());
    }

    @Test
    public void afterErrorDoesNotReturnSuccessfulCore() {
        IllegalStateException hook = new IllegalStateException("hook");
        Processor<String, String> around =
                AroundProcessor.<String, String>builder()
                        .delegate(input -> of("ok"))
                        .afterHandler(result -> Handled.error(hook))
                        .build();

        Processed<String> result = around.process("x");

        assertTrue(result.isError());
        assertSame(hook, result.exception());
        assertFalse(result.hasValue());
    }

    @Test
    public void afterErrorWhenCoreFailedSuppressesCoreException() {
        IllegalStateException core = new IllegalStateException("core");
        IllegalStateException hook = new IllegalStateException("hook");
        Processor<String, String> around =
                AroundProcessor.<String, String>builder()
                        .delegate(input -> Processed.error(core))
                        .afterHandler(result -> Handled.error(hook))
                        .build();

        Processed<String> result = around.process("x");

        assertTrue(result.isError());
        assertSame(hook, result.exception());
        assertArrayEquals(new Throwable[] {core}, result.exception().getSuppressed());
    }
}
