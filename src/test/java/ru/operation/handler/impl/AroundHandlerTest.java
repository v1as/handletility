package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class AroundHandlerTest {

    @Test
    public void afterReceivesMainResultAndDoesNotReplaceIt() {
        List<String> order = new ArrayList<>();
        AtomicReference<Handled> seenAfter = new AtomicReference<>();
        Handler<String> around =
                AroundHandler.<String>builder()
                        .beforeHandler(new ConsumerHandler<>((String s) -> order.add("before")))
                        .delegate(
                                new ConsumerHandler<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(
                                result -> {
                                    order.add("after");
                                    seenAfter.set(result);
                                    return skipped();
                                })
                        .build();

        Handled result = around.handle("x");

        assertTrue(result.isError());
        assertEquals("boom", result.exception().getMessage());
        assertSame(result, seenAfter.get());
        assertEquals(List.of("before", "main", "after"), order);
    }

    @Test
    public void returnsMainHandlerResult() {
        Handler<String> around = AroundHandler.<String>builder().delegate(input -> done()).build();

        assertTrue(around.handle("ok").isDone());
    }

    @Test
    public void skippedAndDoneHooksDoNotReplaceCore() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        Handler<String> skippedBefore =
                AroundHandler.<String>builder()
                        .beforeHandler(input -> skipped())
                        .delegate(
                                input -> {
                                    coreCalled.set(true);
                                    return error(new IllegalStateException("core"));
                                })
                        .build();

        Handled skippedBeforeResult = skippedBefore.handle("x");

        assertTrue(coreCalled.get());
        assertTrue(skippedBeforeResult.isError());
        assertEquals("core", skippedBeforeResult.exception().getMessage());

        Handler<String> doneAfter =
                AroundHandler.<String>builder()
                        .delegate(input -> skipped())
                        .afterHandler(result -> done())
                        .build();

        assertTrue(doneAfter.handle("x").isSkipped());
    }

    @Test
    public void beforeErrorSkipsCoreAndAfter() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicBoolean afterCalled = new AtomicBoolean();
        IOException hook = new IOException("hook");
        Handled hookError = error(hook);
        Handler<String> around =
                AroundHandler.<String>builder()
                        .beforeHandler(input -> hookError)
                        .delegate(
                                input -> {
                                    coreCalled.set(true);
                                    return done();
                                })
                        .afterHandler(
                                result -> {
                                    afterCalled.set(true);
                                    return done();
                                })
                        .build();

        assertSame(hookError, around.handle("x"));
        assertFalse(coreCalled.get());
        assertFalse(afterCalled.get());
    }

    @Test
    public void afterErrorDoesNotReturnSuccessfulCore() {
        Handled hookError = error(new IllegalStateException("hook"));
        Handler<String> around =
                AroundHandler.<String>builder()
                        .delegate(input -> done())
                        .afterHandler(result -> hookError)
                        .build();

        assertSame(hookError, around.handle("x"));
    }

    @Test
    public void afterErrorWhenCoreFailedSuppressesCoreException() {
        IllegalStateException core = new IllegalStateException("core");
        IllegalStateException hook = new IllegalStateException("hook");
        Handled hookError = error(hook);
        Handler<String> around =
                AroundHandler.<String>builder()
                        .delegate(input -> error(core))
                        .afterHandler(result -> hookError)
                        .build();

        Handled result = around.handle("x");

        assertSame(hookError, result);
        assertArrayEquals(new Throwable[] {core}, result.exception().getSuppressed());
    }
}
