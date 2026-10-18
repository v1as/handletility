package ru.operation.processor.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.of;
import static ru.operation.processor.Processed.skipped;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.LoggingExceptionProcessor;

public class ProcessorListTest {

    @Test
    public void should_process_after_skip() {
        Processor<String, String> handlers =
                new ProcessorList<>(List.of(value -> skipped(), value -> of("polo")));
        Processed<String> result = handlers.process("marko");
        assertEquals(of("polo"), result);
    }

    @Test
    public void continueOnErrorTriesNext() {
        Processor<String, Integer> list =
                new ProcessorList<>(
                        List.of(input -> error("first"), input -> of(7)),
                        Set.of(ProcessorModifier.CONTINUE_ON_ERROR),
                        new LoggingExceptionProcessor<>());

        Processed<Integer> result = list.process("x");
        assertTrue(result.isDone());
        assertEquals(7, result.value());
    }
}
