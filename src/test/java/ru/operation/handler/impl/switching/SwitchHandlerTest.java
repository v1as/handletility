package ru.operation.handler.impl.switching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerBuilder;
import ru.operation.handler.IdentifiedHandler;
import ru.operation.processor.impl.FunctionProcessor;

public class SwitchHandlerTest {

    private final int[] store = {-1};

    private final IdentifiedHandler<String, String> firstHandler = marking(1).identifiedBy("1");

    private final IdentifiedHandler<String, String> secondHandler = marking(2).identifiedBy("2");

    private final Handler<String> forthHandler =
            (input) -> {
                store[0] = 4;
                return done();
            };

    private final FunctionProcessor<String, String> identityKeyReader =
            new FunctionProcessor<>(s -> s);

    private Handler<String> marking(int mark) {
        return input -> {
            store[0] = mark;
            return done();
        };
    }

    @Test
    public void shouldHandleWithNoDefault() {
        Handler<String> handler =
                HandlerBuilder.from(
                                SwitchHandler.<String, String>switchHandler()
                                        .handler(firstHandler)
                                        .handler(secondHandler)
                                        .keyReader(identityKeyReader)
                                        .build())
                        .logging()
                        .build();

        assertTrue(handler.handle("3").isSkipped());
        assertEquals(-1, store[0]);

        assertTrue(handler.handle("1").isDone());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isDone());
        assertEquals(2, store[0]);
    }

    @Test
    public void shouldHandleWithDefault() {
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .handler(secondHandler)
                        .keyReader(identityKeyReader)
                        .defaultHandler(forthHandler)
                        .build();

        assertTrue(handler.handle("abc").isDone());
        assertEquals(4, store[0]);

        assertTrue(handler.handle("1").isDone());
        assertEquals(1, store[0]);

        assertTrue(handler.handle("2").isDone());
        assertEquals(2, store[0]);
    }

    @Test
    public void keyReaderErrorDoesNotCallDefault() {
        IllegalStateException failure = new IllegalStateException("broken parse");
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .keyReader(
                                new FunctionProcessor<>(
                                        input -> {
                                            throw failure;
                                        }))
                        .defaultHandler(
                                input -> {
                                    defaultCalled.set(true);
                                    return done();
                                })
                        .build();

        Handled result = handler.handle("weird");

        assertTrue(result.isError());
        assertSame(failure, result.exception());
        assertFalse(defaultCalled.get());
    }

    @Test
    public void skippedKeyReaderUsesDefault() {
        AtomicBoolean defaultCalled = new AtomicBoolean();
        SwitchHandler<String, String> handler =
                SwitchHandler.<String, String>switchHandler()
                        .handler(firstHandler)
                        .keyReader(new FunctionProcessor<>(input -> null))
                        .defaultHandler(
                                input -> {
                                    defaultCalled.set(true);
                                    return done();
                                })
                        .build();

        assertTrue(handler.handle("weird").isDone());
        assertTrue(defaultCalled.get());
    }
}
