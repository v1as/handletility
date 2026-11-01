package ru.operation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CollectionChecksTest {

    @Test
    public void isNullOrEmpty() {
        assertTrue(CollectionChecks.isNullOrEmpty(null));
        assertTrue(CollectionChecks.isNullOrEmpty(List.of()));
        assertFalse(CollectionChecks.isNullOrEmpty(List.of(1)));
    }

    @Test
    public void copyNonEmptyCopies() {
        assertEquals(List.of("a"), CollectionChecks.copyNonEmpty(List.of("a"), "items"));
    }

    @Test
    public void copyNonEmptyRejectsNullOrEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionChecks.copyNonEmpty(null, "mdc entries"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionChecks.copyNonEmpty(List.of(), "mdc entries"));
    }
}
