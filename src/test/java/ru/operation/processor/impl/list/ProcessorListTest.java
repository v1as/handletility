package ru.operation.processor.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.of;
import static ru.operation.processor.Processed.skipped;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
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

    @Test
    public void singleErrorIsTheOriginalException() {
        IOException checked = new IOException("disk");
        Processed<String> checkedResult =
                new ProcessorList<String, String>(List.of(input -> error(checked))).process("x");
        assertSame(checked, checkedResult.exception());
        ProcessorException wrapped =
                assertThrows(ProcessorException.class, checkedResult::orElseThrow);
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
        assertEquals(0, wrapped.getSuppressed().length);

        IllegalStateException runtime = new IllegalStateException("disk");
        Processed<String> runtimeResult =
                new ProcessorList<String, String>(List.of(input -> error(runtime))).process("x");
        assertSame(runtime, runtimeResult.exception());
        assertSame(runtime, assertThrows(IllegalStateException.class, runtimeResult::orElseThrow));
    }

    @Test
    public void continuedErrorsKeepEachOriginal() {
        IOException first = new IOException("disk");
        IllegalArgumentException second = new IllegalArgumentException("bad");
        Processor<String, String> list =
                new ProcessorList<>(
                        List.of(input -> error(first), input -> error(second)),
                        Set.of(ProcessorModifier.CONTINUE_ON_ERROR),
                        new LoggingExceptionProcessor<>());

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(List.of(first, second), aggregated.causes());
        assertSame(first, aggregated.getCause());
        assertEquals("disk; bad", aggregated.getMessage());
        assertEquals(0, aggregated.getSuppressed().length);
        assertSame(aggregated, assertThrows(ProcessorException.class, result::orElseThrow));
    }

    @Test
    public void nullExceptionIsNotSuppressed() {
        Processed<String> result =
                new ProcessorList<String, String>(List.of(input -> error((Exception) null)))
                        .process("x");
        assertTrue(result.isError());
        assertNull(result.exception());
    }
}
