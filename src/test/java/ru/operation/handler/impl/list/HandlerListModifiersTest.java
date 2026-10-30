package ru.operation.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.ok;
import static ru.operation.handler.Handled.skipped;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.HandlerException;

public class HandlerListModifiersTest {

    @Test
    public void stopsOnFirstDoneByDefault() {
        List<String> called = new ArrayList<>();
        Handler<String> list =
                new HandlerList<>(
                        List.of(
                                input -> {
                                    called.add("first");
                                    return ok();
                                },
                                input -> {
                                    called.add("second");
                                    return ok();
                                }));

        assertTrue(list.handle("x").isHandled());
        assertEquals(List.of("first"), called);
    }

    @Test
    public void continueOnDoneRunsAll() {
        List<String> called = new ArrayList<>();
        Handler<String> list =
                HandlerList.<String>builder()
                        .handler(
                                input -> {
                                    called.add("first");
                                    return ok();
                                })
                        .handler(
                                input -> {
                                    called.add("second");
                                    return skipped();
                                })
                        .modifier(HandlerModifier.CONTINUE_ON_DONE)
                        .build();

        assertTrue(list.handle("x").isHandled());
        assertEquals(List.of("first", "second"), called);
    }

    @Test
    public void continueOnErrorCollectsFailures() {
        HandlerException first = new HandlerException("one");
        HandlerException second = new HandlerException("two");
        Handler<String> list =
                HandlerList.<String>builder()
                        .handler(input -> error(first))
                        .handler(input -> error(second))
                        .handler(input -> ok())
                        .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                        .modifier(HandlerModifier.CONTINUE_ON_DONE)
                        .build();

        Handled result = list.handle("x");
        HandlerException aggregated = assertInstanceOf(HandlerException.class, result.exception());
        assertEquals(List.of(first, second), aggregated.causes());
        assertSame(first, aggregated.getCause());
        assertEquals("one; two", aggregated.getMessage());
        assertEquals(0, aggregated.getSuppressed().length);
        assertSame(aggregated, assertThrows(HandlerException.class, result::throwIfError));
    }

    @Test
    public void stopsOnErrorWithoutModifier() {
        List<String> called = new ArrayList<>();
        Handler<String> list =
                new HandlerList<>(
                        List.of(
                                input -> {
                                    called.add("first");
                                    return error("fail");
                                },
                                input -> {
                                    called.add("second");
                                    return ok();
                                }));

        assertTrue(list.handle("x").isError());
        assertEquals(List.of("first"), called);
    }

    @Test
    public void singleErrorIsTheOriginalException() {
        IOException checked = new IOException("disk");
        Handled checkedResult = new HandlerList<>(List.of(input -> error(checked))).handle("x");
        assertSame(checked, checkedResult.exception());
        HandlerException wrapped =
                assertThrows(HandlerException.class, checkedResult::throwIfError);
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
        assertEquals(0, wrapped.getSuppressed().length);

        IllegalStateException runtime = new IllegalStateException("disk");
        Handled runtimeResult = new HandlerList<>(List.of(input -> error(runtime))).handle("x");
        assertSame(runtime, runtimeResult.exception());
        assertSame(runtime, assertThrows(IllegalStateException.class, runtimeResult::throwIfError));
    }

    @Test
    public void continuedErrorsKeepEachOriginal() {
        IOException first = new IOException("disk");
        IllegalArgumentException second = new IllegalArgumentException("bad");
        Handler<String> list =
                HandlerList.<String>builder()
                        .handler(input -> error(first))
                        .handler(input -> error(second))
                        .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                        .build();

        Handled result = list.handle("x");
        HandlerException aggregated = assertInstanceOf(HandlerException.class, result.exception());
        assertEquals(List.of(first, second), aggregated.causes());
        assertSame(first, aggregated.getCause());
        assertEquals("disk; bad", aggregated.getMessage());
        assertEquals(0, aggregated.getSuppressed().length);
        assertSame(aggregated, assertThrows(HandlerException.class, result::throwIfError));
    }
}
