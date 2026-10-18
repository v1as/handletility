package ru.operation.handler.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class AsyncHandlerTest {

    @Test
    public void shouldExecuteAsync() {
        AtomicInteger value = new AtomicInteger(-1);
        Handler<Integer> handler =
                new AsyncHandler<>(
                        Executors.newSingleThreadExecutor(),
                        new SimpleHandler<>(
                                i -> i > 0,
                                i -> {
                                    Thread.sleep(500);
                                    value.set(i);
                                }));
        await().atMost(200, MILLISECONDS).until(() -> handler.handle(-1).isSkipped());

        Handled immediate = handler.handle(3);
        assertTrue(immediate.isHandled());
        await().atLeast(400, MILLISECONDS).until(() -> value.get() == 3);
    }

    @Test
    public void backgroundErrorDoesNotFailImmediateResult() {
        AtomicBoolean finished = new AtomicBoolean(false);
        Handler<Integer> handler =
                new AsyncHandler<>(
                        Executors.newSingleThreadExecutor(),
                        new SimpleHandler<>(
                                i -> {
                                    finished.set(true);
                                    throw new IllegalStateException("async fail");
                                }));

        Handled immediate = handler.handle(1);
        assertTrue(immediate.isHandled());
        await().atMost(2, SECONDS).untilTrue(finished);
    }
}
