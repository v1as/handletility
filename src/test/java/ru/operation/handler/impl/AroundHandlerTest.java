package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.ok;

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
                        .beforeHandler(new SimpleHandler<>((String s) -> order.add("before")))
                        .handler(
                                new SimpleHandler<>(
                                        s -> {
                                            order.add("main");
                                            throw new IllegalStateException("boom");
                                        }))
                        .afterHandler(
                                result -> {
                                    order.add("after");
                                    seenAfter.set(result);
                                    return Handled.skipped();
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
        Handler<String> around = AroundHandler.<String>builder().handler(input -> ok()).build();

        assertTrue(around.handle("ok").isHandled());
    }

    @Test
    public void skippedAndDoneHooksDoNotReplaceCore() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        Handler<String> skippedBefore =
                AroundHandler.<String>builder()
                        .beforeHandler(input -> Handled.skipped())
                        .handler(
                                input -> {
                                    coreCalled.set(true);
                                    return Handled.error(new IllegalStateException("core"));
                                })
                        .build();

        Handled skippedBeforeResult = skippedBefore.handle("x");

        assertTrue(coreCalled.get());
        assertTrue(skippedBeforeResult.isError());
        assertEquals("core", skippedBeforeResult.exception().getMessage());

        Handler<String> doneAfter =
                AroundHandler.<String>builder()
                        .handler(input -> Handled.skipped())
                        .afterHandler(result -> ok())
                        .build();

        assertTrue(doneAfter.handle("x").isSkipped());
    }

    @Test
    public void beforeErrorSkipsCoreAndAfter() {
        AtomicBoolean coreCalled = new AtomicBoolean();
        AtomicBoolean afterCalled = new AtomicBoolean();
        IOException hook = new IOException("hook");
        Handled hookError = Handled.error(hook);
        Handler<String> around =
                AroundHandler.<String>builder()
                        .beforeHandler(input -> hookError)
                        .handler(
                                input -> {
                                    coreCalled.set(true);
                                    return ok();
                                })
                        .afterHandler(
                                result -> {
                                    afterCalled.set(true);
                                    return ok();
                                })
                        .build();

        assertSame(hookError, around.handle("x"));
        assertFalse(coreCalled.get());
        assertFalse(afterCalled.get());
    }

    @Test
    public void afterErrorDoesNotReturnSuccessfulCore() {
        Handled hookError = Handled.error(new IllegalStateException("hook"));
        Handler<String> around =
                AroundHandler.<String>builder()
                        .handler(input -> ok())
                        .afterHandler(result -> hookError)
                        .build();

        assertSame(hookError, around.handle("x"));
    }

    @Test
    public void afterErrorWhenCoreFailedSuppressesCoreException() {
        IllegalStateException core = new IllegalStateException("core");
        IllegalStateException hook = new IllegalStateException("hook");
        Handled hookError = Handled.error(hook);
        Handler<String> around =
                AroundHandler.<String>builder()
                        .handler(input -> Handled.error(core))
                        .afterHandler(result -> hookError)
                        .build();

        Handled result = around.handle("x");

        assertSame(hookError, result);
        assertArrayEquals(new Throwable[] {core}, result.exception().getSuppressed());
    }
}
