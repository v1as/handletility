package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.operation.ResultState;
import ru.operation.processor.Processed;

public class HandledTest {

    @Test
    public void doneIsDone() {
        assertTrue(Handled.done().isDone());
        assertFalse(Handled.skipped().isDone());
        assertSame(Handled.done(), Handled.done());
    }

    @Test
    public void throwIfErrorOnError() {
        HandlerException error = new HandlerException("boom");
        assertSame(
                error,
                assertThrows(HandlerException.class, () -> Handled.error(error).throwIfError()));

        IOException checked = new IOException("disk");
        HandlerException wrapped =
                assertThrows(HandlerException.class, () -> Handled.error(checked).throwIfError());
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
    }

    @Test
    public void throwIfErrorPassesThroughOkAndSkipped() {
        assertSame(Handled.done(), Handled.done().throwIfError());
        assertSame(Handled.skipped(), Handled.skipped().throwIfError());
    }

    @Test
    public void requireDoneRejectsSkip() {
        Handled.done().requireDone();

        HandlerException skipped =
                assertThrows(HandlerException.class, Handled.skipped()::requireDone);
        assertNull(skipped.getCause());

        HandlerException error = new HandlerException("boom");
        assertSame(
                error,
                assertThrows(HandlerException.class, () -> Handled.error(error).requireDone()));
    }

    @Test
    public void thenGetSuppliesOnlyWhenHandled() {
        assertEquals("ok", Handled.done().thenGet(() -> "ok").requireDone());
        assertTrue(Handled.done().thenGet(() -> null).isDone());
        assertFalse(Handled.done().thenGet(() -> null).hasValue());

        assertTrue(Handled.skipped().thenGet(Assertions::fail).isSkipped());

        Exception boom = new HandlerException("boom");
        Processed<String> error = Handled.error(boom).thenGet(() -> fail());
        assertTrue(error.isError());
        assertSame(boom, error.exception());

        Processed<String> failed =
                Handled.done()
                        .thenGet(
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
        assertThrows(IllegalArgumentException.class, () -> Handled.error((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> new Handled(ResultState.DONE, boom));
        assertThrows(IllegalArgumentException.class, () -> new Handled(ResultState.SKIPPED, boom));
        assertThrows(IllegalArgumentException.class, () -> new Handled(null, null));
    }
}
