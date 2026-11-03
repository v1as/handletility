package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.operation.ResultState;
import ru.operation.processor.Processed;

public class HandledTest {

    @Test
    public void doneIsDone() {
        assertTrue(done().isDone());
        assertFalse(skipped().isDone());
        assertSame(done(), done());
    }

    @Test
    public void throwIfErrorOnError() {
        HandlerException error = new HandlerException("boom");
        assertSame(error, assertThrows(HandlerException.class, () -> error(error).throwIfError()));

        IOException checked = new IOException("disk");
        HandlerException wrapped =
                assertThrows(HandlerException.class, () -> error(checked).throwIfError());
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
    }

    @Test
    public void throwIfErrorPassesThroughDoneAndSkipped() {
        assertSame(done(), done().throwIfError());
        assertSame(skipped(), skipped().throwIfError());
    }

    @Test
    public void requireDoneRejectsSkip() {
        done().requireDone();

        HandlerException skipped = assertThrows(HandlerException.class, skipped()::requireDone);
        assertNull(skipped.getCause());

        HandlerException error = new HandlerException("boom");
        assertSame(error, assertThrows(HandlerException.class, () -> error(error).requireDone()));
    }

    @Test
    public void produceSuppliesOnlyWhenHandled() {
        assertEquals("ok", done().produce(() -> "ok").requireDone());
        assertTrue(done().produce(() -> null).isDone());
        assertFalse(done().produce(() -> null).hasValue());

        assertTrue(skipped().produce(Assertions::fail).isSkipped());

        Exception boom = new HandlerException("boom");
        Processed<String> error = error(boom).produce(() -> fail());
        assertTrue(error.isError());
        assertSame(boom, error.exception());

        Processed<String> failed =
                done().produce(
                                () -> {
                                    throw new IllegalStateException("nope");
                                });
        assertTrue(failed.isError());
        assertEquals("nope", failed.exception().getMessage());
    }

    @Test
    public void constructorRejectsInvalidCombinations() {
        IllegalStateException boom = new IllegalStateException("boom");

        assertThrows(IllegalArgumentException.class, () -> new Handled(ResultState.ERROR, null));
        assertThrows(IllegalArgumentException.class, () -> error((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> new Handled(ResultState.DONE, boom));
        assertThrows(IllegalArgumentException.class, () -> new Handled(ResultState.SKIPPED, boom));
        assertThrows(IllegalArgumentException.class, () -> new Handled(null, null));
    }
}
