package ru.operation.handler.impl.wrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.operation.handler.impl.SimpleHandler;

public class NamedHandlerTest {

    @Test
    public void delegatesAndExposesName() {
        var core = new SimpleHandler<String>(s -> {});
        var named = new NamedHandler<>("persist", core);

        assertEquals("persist", named.getName());
        assertTrue(named.handle("ok").isDone());
        assertEquals("Named[persist](" + core + ")", named.toString());
    }
}
