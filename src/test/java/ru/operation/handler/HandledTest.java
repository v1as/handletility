package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HandledTest {

    @Test
    public void okIsHandled() {
        assertTrue(Handled.ok().isHandled());
        assertSame(Handled.ok(), Handled.ok());
    }

    @Test
    public void orElseThrowOnError() {
        Handled error = Handled.error("boom");
        HandlerException thrown = assertThrows(HandlerException.class, error::orElseThrow);
        assertEquals("Handling error", thrown.getMessage());
        assertSame(error.exception(), thrown.getCause());
    }

    @Test
    public void orElseThrowPassesThroughOkAndSkipped() {
        assertSame(Handled.ok(), Handled.ok().orElseThrow());
        assertSame(Handled.skipped(), Handled.skipped().orElseThrow());
    }
}
