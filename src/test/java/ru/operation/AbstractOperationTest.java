package ru.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import ru.operation.handler.impl.SimpleHandler;

public class AbstractOperationTest {

    @Test
    public void toStringUsesClassNameWithoutRecursion() {
        var handler = new SimpleHandler<String>(s -> {});
        assertEquals("SimpleHandler", handler.toString());

        handler.setName("persist");
        assertEquals("SimpleHandler[persist]", handler.toString());
        assertEquals("persist", handler.getName());
    }

    @Test
    public void withNameRejectsNull() {
        var handler = new SimpleHandler<String>(s -> {});
        assertThrows(
                IllegalArgumentException.class, () -> AbstractOperation.withName(handler, null));
    }
}
