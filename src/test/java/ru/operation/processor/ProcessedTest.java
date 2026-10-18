package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.operation.ResultState;

public class ProcessedTest {

    @Test
    public void stateHelpers() {
        assertTrue(Processed.processed(1).isDone());
        assertFalse(Processed.processed(1).isError());
        assertFalse(Processed.processed(1).isSkipped());

        assertTrue(Processed.skipped().isSkipped());
        assertTrue(Processed.skipped().isEmpty());

        Processed<Integer> error = Processed.error("fail");
        assertTrue(error.isError());
        assertEquals(ResultState.ERROR, error.state());
        assertThrows(ProcessorException.class, error::orThrow);
    }
}
