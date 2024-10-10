package ru.operation.processor.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.operation.processor.Processed.processed;
import static ru.operation.processor.Processed.skipped;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;

public class ProcessorListTest {

    @Test
    public void should_process_after_skip() {
        Processor<String, String> handlers =
                new ProcessorList<>(List.of(value -> skipped(), value -> processed("polo")));
        Processed<String> result = handlers.process("marko");
        assertEquals(processed("polo"), result);
    }
}
