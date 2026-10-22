package ru.operation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CollectionsTest {

    @Test
    public void isNullOrEmpty() {
        assertTrue(Collections.isNullOrEmpty(null));
        assertTrue(Collections.isNullOrEmpty(List.of()));
        assertFalse(Collections.isNullOrEmpty(List.of(1)));
    }

    @Test
    public void copyNonEmptyCopies() {
        assertEquals(List.of("a"), Collections.copyNonEmpty(List.of("a"), "items"));
    }

    @Test
    public void copyNonEmptyRejectsNullOrEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Collections.copyNonEmpty(null, "mdc entries"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Collections.copyNonEmpty(List.of(), "mdc entries"));
    }
}
