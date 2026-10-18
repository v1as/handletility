package ru.operation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ChecksTest {

    @Test
    public void notNullReturnsValue() {
        assertEquals("x", Checks.notNull("x", "name"));
    }

    @Test
    public void notNullRejectsNull() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Checks.notNull(null, "name"));
        assertEquals("name cannot be null", ex.getMessage());
    }

    @Test
    public void isNullOrEmpty() {
        assertTrue(Checks.isNullOrEmpty(null));
        assertTrue(Checks.isNullOrEmpty(""));
        assertFalse(Checks.isNullOrEmpty("a"));
    }
}
