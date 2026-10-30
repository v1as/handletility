package ru.operation.processor.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.of;
import static ru.operation.processor.Processed.skipped;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
import ru.operation.util.function.ThrowableBiFunction;

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
                ProcessorList.<String, Integer>processorList()
                        .processor(input -> error("first"))
                        .processor(input -> of(7))
                        .modifier(ProcessorModifier.CONTINUE_ON_ERROR)
                        .build();

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
                assertThrows(ProcessorException.class, checkedResult::throwIfError);
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
        assertEquals(0, wrapped.getSuppressed().length);

        IllegalStateException runtime = new IllegalStateException("disk");
        Processed<String> runtimeResult =
                new ProcessorList<String, String>(List.of(input -> error(runtime))).process("x");
        assertSame(runtime, runtimeResult.exception());
        assertSame(runtime, assertThrows(IllegalStateException.class, runtimeResult::throwIfError));
    }

    @Test
    public void continuedErrorsKeepEachOriginal() {
        IOException first = new IOException("disk");
        IllegalArgumentException second = new IllegalArgumentException("bad");
        Processor<String, String> list =
                ProcessorList.<String, String>processorList()
                        .processor(input -> error(first))
                        .processor(input -> error(second))
                        .modifier(ProcessorModifier.CONTINUE_ON_ERROR)
                        .build();

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(List.of(first, second), aggregated.causes());
        assertSame(first, aggregated.getCause());
        assertEquals("disk; bad", aggregated.getMessage());
        assertEquals(0, aggregated.getSuppressed().length);
        assertSame(aggregated, assertThrows(ProcessorException.class, result::throwIfError));
    }

    @Test
    public void foldSkipsDoNotEnterTheAccumulator() {
        Processor<String, String> list =
                folding(
                        List.of(
                                input -> skipped(),
                                input -> of("a"),
                                input -> skipped(),
                                input -> of("b")),
                        (left, right) -> left + right,
                        false);

        assertEquals(of("ab"), list.process("x"));
    }

    @Test
    public void foldReturnsSingleDoneWithoutCombining() {
        Processor<String, String> list =
                folding(
                        List.of(input -> skipped(), input -> of(null)),
                        (left, right) -> {
                            throw new AssertionError("combined");
                        },
                        false);

        assertEquals(of(null), list.process("x"));
    }

    @Test
    public void foldCombinesDoneLeftToRight() {
        Processor<String, String> list =
                folding(
                        List.of(input -> of("a"), input -> of("b"), input -> of("c")),
                        (left, right) -> "(" + left + right + ")",
                        false);

        assertEquals(of("((ab)c)"), list.process("x"));
    }

    @Test
    public void foldErrorDropsAccumulatorAndStops() {
        IOException disk = new IOException("disk");
        boolean[] later = {false};
        Processor<String, String> list =
                folding(
                        List.of(
                                input -> of("a"),
                                input -> error(disk),
                                input -> {
                                    later[0] = true;
                                    return of("b");
                                }),
                        (left, right) -> left + right,
                        false);

        Processed<String> result = list.process("x");
        assertSame(disk, result.exception());
        assertFalse(later[0]);
    }

    @Test
    public void foldContinueOnErrorDropsAccumulator() {
        IOException first = new IOException("disk");
        IllegalArgumentException second = new IllegalArgumentException("bad");
        Processor<String, String> list =
                folding(
                        List.of(
                                input -> of("a"),
                                input -> error(first),
                                input -> of("b"),
                                input -> error(second)),
                        (left, right) -> left + right,
                        true);

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(List.of(first, second), aggregated.causes());
        assertTrue(result.isError());
    }

    @Test
    public void foldCombineExceptionBecomesError() {
        IOException merge = new IOException("merge");
        boolean[] later = {false};
        Processor<String, String> list =
                folding(
                        List.of(
                                input -> of("a"),
                                input -> of("b"),
                                input -> {
                                    later[0] = true;
                                    return of("c");
                                }),
                        (left, right) -> {
                            throw merge;
                        },
                        false);

        assertSame(merge, list.process("x").exception());
        assertFalse(later[0]);
    }

    @Test
    public void foldContinueOnErrorKeepsCombineException() {
        IOException disk = new IOException("disk");
        IOException merge = new IOException("merge");
        boolean[] later = {false};
        Processor<String, String> list =
                folding(
                        List.of(
                                input -> of("a"),
                                input -> error(disk),
                                input -> of("b"),
                                input -> {
                                    later[0] = true;
                                    return of("c");
                                }),
                        (left, right) -> {
                            if ("b".equals(right)) {
                                throw merge;
                            }
                            return left + right;
                        },
                        true);

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(List.of(disk, merge), aggregated.causes());
        assertTrue(later[0]);
    }

    private static <O> Processor<String, O> folding(
            List<? extends Processor<String, O>> processors,
            ThrowableBiFunction<O, O, O> combiner,
            boolean continueOnError) {
        ProcessorList.ProcessorListBuilder<String, O> builder =
                ProcessorList.<String, O>processorList().combiner(combiner);
        processors.forEach(builder::processor);
        if (continueOnError) {
            builder.modifier(ProcessorModifier.CONTINUE_ON_ERROR);
        }
        return builder.build();
    }
}
