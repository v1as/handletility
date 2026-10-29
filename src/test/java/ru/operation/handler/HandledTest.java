package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;

public class HandledTest {

    @Test
    public void okIsHandled() {
        assertTrue(Handled.ok().isHandled());
        assertSame(Handled.ok(), Handled.ok());
    }

    @Test
    public void orElseThrowOnError() {
        HandlerException error = new HandlerException("boom");
        assertSame(
                error,
                assertThrows(HandlerException.class, () -> Handled.error(error).orElseThrow()));

        IOException checked = new IOException("disk");
        HandlerException wrapped =
                assertThrows(HandlerException.class, () -> Handled.error(checked).orElseThrow());
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
    }

    @Test
    public void orElseThrowPassesThroughOkAndSkipped() {
        assertSame(Handled.ok(), Handled.ok().orElseThrow());
        assertSame(Handled.skipped(), Handled.skipped().orElseThrow());
    }

    @Test
    public void thenGetSuppliesOnlyWhenHandled() {
        assertEquals("ok", Handled.ok().thenGet(() -> "ok").orElseThrow());
        assertTrue(Handled.ok().thenGet(() -> null).isSkipped());

        assertTrue(Handled.skipped().thenGet(Assertions::fail).isSkipped());

        Exception boom = new HandlerException("boom");
        Processed<String> error = Handled.error(boom).thenGet(() -> fail());
        assertTrue(error.isError());
        assertSame(boom, error.exception());

        Processed<String> failed =
                Handled.ok()
                        .thenGet(
                                () -> {
                                    throw new IllegalStateException("nope");
                                });
        assertTrue(failed.isError());
        assertEquals("nope", failed.exception().getMessage());
    }
}
