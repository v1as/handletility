package ru.operation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CollsTest {

    @Test
    public void isNullOrEmpty() {
        assertTrue(Colls.isNullOrEmpty(null));
        assertTrue(Colls.isNullOrEmpty(List.of()));
        assertFalse(Colls.isNullOrEmpty(List.of(1)));
    }

    @Test
    public void copyNonEmptyCopies() {
        assertEquals(List.of("a"), Colls.copyNonEmpty(List.of("a"), "items"));
    }

    @Test
    public void copyNonEmptyRejectsNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Colls.copyNonEmpty(null, "mdc entries"));
        assertThrows(
                IllegalArgumentException.class, () -> Colls.copyNonEmpty(List.of(), "mdc entries"));
    }
}
