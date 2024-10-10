package ru.operation.handler.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;

public class AsyncHandlerTest {

    @Test
    public void shouldExecuteAsync() {
        int[] value = {-1};
        Handler<Integer> handler =
                new AsyncHandler<>(
                        Executors.newSingleThreadExecutor(),
                        new SimpleHandler<>(
                                i -> i > 0,
                                i -> {
                                    Thread.sleep(500);
                                    value[0] = i;
                                }));
        await().atMost(200, MILLISECONDS).until(() -> handler.handle(-1).isSkipped());

        handler.handle(3);
        await().atLeast(400, MILLISECONDS).until(() -> value[0] == 3);
    }
}
