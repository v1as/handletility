package ru.operation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class CollectionChecksTest {

    @Test
    public void isNullOrEmpty() {
        assertTrue(CollectionChecks.isNullOrEmpty(null));
        assertTrue(CollectionChecks.isNullOrEmpty(Collections.emptyList()));
        assertFalse(CollectionChecks.isNullOrEmpty(Arrays.asList(1)));
    }

    @Test
    public void copyNonEmptyCopies() {
        assertEquals(
                Arrays.asList("a"), CollectionChecks.copyNonEmpty(Arrays.asList("a"), "items"));
    }

    @Test
    public void copyNonEmptyRejectsNullOrEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionChecks.copyNonEmpty(null, "mdc entries"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionChecks.copyNonEmpty(Collections.emptyList(), "mdc entries"));
    }
}
