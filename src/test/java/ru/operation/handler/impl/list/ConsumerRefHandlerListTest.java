package ru.operation.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import ru.operation.handler.Handler;

public class ConsumerRefHandlerListTest {

    @Test
    public void shouldReferenceMethods() {
        SeveralMethodsHandler first = new SeveralMethodsHandler();
        SeveralMethodsHandler second = new SeveralMethodsHandler();
        var severalEventsHandlers = List.of(first, second);

        Handler<Integer> intMethodHandler =
                new ConsumerRefHandlerList<>(
                        severalEventsHandlers, SeveralMethodsHandler::handleInteger);

        Handler<String> stringMethodHandler =
                new ConsumerRefHandlerList<>(
                        severalEventsHandlers, SeveralMethodsHandler::handleString);

        intMethodHandler.handle(123);
        assertEquals(List.of(123), first.getInts());
        assertEquals(List.of(123), second.getInts());

        stringMethodHandler.handle("hello");
        assertEquals(List.of("hello"), first.getStrings());
        assertEquals(List.of("hello"), second.getStrings());
    }

    @Getter
    private static class SeveralMethodsHandler {

        private final List<String> strings = new ArrayList<>();
        private final List<Integer> ints = new ArrayList<>();

        public void handleString(String value) {
            strings.add(value);
        }

        public void handleInteger(Integer value) {
            ints.add(value);
        }
    }
}
