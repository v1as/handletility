package ru.operation.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.handled;
import static ru.operation.handler.Handled.skipped;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

public class HandlerListModifiersTest {

    @Test
    public void stopsOnFirstDoneByDefault() {
        List<String> called = new ArrayList<>();
        Handler<String> list =
                new HandlerList<>(
                        List.of(
                                input -> {
                                    called.add("first");
                                    return handled();
                                },
                                input -> {
                                    called.add("second");
                                    return handled();
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
                                    return handled();
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
        Handler<String> list =
                HandlerList.<String>builder()
                        .handler(input -> error("one"))
                        .handler(input -> error("two"))
                        .handler(input -> handled())
                        .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                        .modifier(HandlerModifier.CONTINUE_ON_DONE)
                        .build();

        Handled result = list.handle("x");
        assertTrue(result.isHandled());
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
                                    return handled();
                                }));

        assertTrue(list.handle("x").isError());
        assertEquals(List.of("first"), called);
    }
}
