package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class AbstractHandlerTest {

    @Test
    public void failedCheckSkipsInternal() {
        AtomicBoolean called = new AtomicBoolean();
        Handler<String> handler =
                new AbstractHandler<>() {
                    @Override
                    public boolean check(String input) {
                        return "ok".equals(input);
                    }

                    @Override
                    protected Handled handleInternal(String input) {
                        called.set(true);
                        return done();
                    }
                };

        assertTrue(handler.handle("no").isSkipped());
        assertFalse(called.get());

        assertTrue(handler.handle("ok").isDone());
        assertTrue(called.get());
    }

    @Test
    public void checkExceptionBecomesErrorAndSkipsInternal() {
        IllegalStateException boom = new IllegalStateException("boom");
        AtomicBoolean called = new AtomicBoolean();
        Handler<String> handler =
                new AbstractHandler<>() {
                    @Override
                    public boolean check(String input) {
                        throw boom;
                    }

                    @Override
                    protected Handled handleInternal(String input) {
                        called.set(true);
                        return done();
                    }
                };

        Handled result = handler.handle("x");
        assertTrue(result.isError());
        assertSame(boom, result.exception());
        assertFalse(called.get());
    }
}
