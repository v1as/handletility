package ru.operation.processor.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.error;
import static ru.operation.processor.Processed.skipped;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.operation.processor.Processed;
import ru.operation.processor.Processor;
import ru.operation.processor.ProcessorException;
import ru.operation.util.function.ThrowableBiFunction;

public class ListProcessorTest {

    @Test
    public void should_process_after_skip() {
        Processor<String, String> handlers =
                new ListProcessor<>(Arrays.asList(value -> skipped(), value -> done("polo")));
        Processed<String> result = handlers.process("marko");
        assertEquals(done("polo"), result);
    }

    @Test
    public void stepTraceIncludesProcessorInputAndResult() {
        Logger logger = (Logger) LoggerFactory.getLogger(ListProcessor.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            new ListProcessor<>(Arrays.asList(new SkippingProcessor(), new DoneProcessor()))
                    .process("marko");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(
                Arrays.asList(
                        "Handling input 'marko'",
                        "Processor 'SkippingProcessor' has result '[SKIPPED]'",
                        "Processor 'DoneProcessor' has result '[DONE: polo]'",
                        "Processed 'marko' to '[DONE: polo]'"),
                appender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList()));
    }

    @Test
    public void continueOnErrorTriesNext() {
        Processor<String, Integer> list =
                ListProcessor.<String, Integer>listProcessor()
                        .processor(input -> error("first"))
                        .processor(input -> done(7))
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
                new ListProcessor<String, String>(Arrays.asList(input -> error(checked)))
                        .process("x");
        assertSame(checked, checkedResult.exception());
        ProcessorException wrapped =
                assertThrows(ProcessorException.class, checkedResult::throwIfError);
        assertEquals("disk", wrapped.getMessage());
        assertSame(checked, wrapped.getCause());
        assertEquals(0, wrapped.getSuppressed().length);

        IllegalStateException runtime = new IllegalStateException("disk");
        Processed<String> runtimeResult =
                new ListProcessor<String, String>(Arrays.asList(input -> error(runtime)))
                        .process("x");
        assertSame(runtime, runtimeResult.exception());
        assertSame(runtime, assertThrows(IllegalStateException.class, runtimeResult::throwIfError));
    }

    @Test
    public void continuedErrorsKeepEachOriginal() {
        IOException first = new IOException("disk");
        IllegalArgumentException second = new IllegalArgumentException("bad");
        Processor<String, String> list =
                ListProcessor.<String, String>listProcessor()
                        .processor(input -> error(first))
                        .processor(input -> error(second))
                        .modifier(ProcessorModifier.CONTINUE_ON_ERROR)
                        .build();

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(Arrays.asList(first, second), aggregated.causes());
        assertSame(first, aggregated.getCause());
        assertEquals("disk; bad", aggregated.getMessage());
        assertEquals(0, aggregated.getSuppressed().length);
        assertSame(aggregated, assertThrows(ProcessorException.class, result::throwIfError));
    }

    @Test
    public void foldSkipsDoNotEnterTheAccumulator() {
        Processor<String, String> list =
                folding(
                        Arrays.asList(
                                input -> skipped(),
                                input -> done("a"),
                                input -> skipped(),
                                input -> done("b")),
                        (left, right) -> left + right,
                        false);

        assertEquals(done("ab"), list.process("x"));
    }

    @Test
    public void foldReturnsSingleDoneWithoutCombining() {
        Processor<String, String> list =
                folding(
                        Arrays.asList(input -> skipped(), input -> done(null)),
                        (left, right) -> {
                            throw new AssertionError("combined");
                        },
                        false);

        assertEquals(done(null), list.process("x"));
    }

    @Test
    public void foldCombinesDoneLeftToRight() {
        Processor<String, String> list =
                folding(
                        Arrays.asList(input -> done("a"), input -> done("b"), input -> done("c")),
                        (left, right) -> "(" + left + right + ")",
                        false);

        assertEquals(done("((ab)c)"), list.process("x"));
    }

    @Test
    public void foldErrorDropsAccumulatorAndStops() {
        IOException disk = new IOException("disk");
        boolean[] later = {false};
        Processor<String, String> list =
                folding(
                        Arrays.asList(
                                input -> done("a"),
                                input -> error(disk),
                                input -> {
                                    later[0] = true;
                                    return done("b");
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
                        Arrays.asList(
                                input -> done("a"),
                                input -> error(first),
                                input -> done("b"),
                                input -> error(second)),
                        (left, right) -> left + right,
                        true);

        Processed<String> result = list.process("x");
        ProcessorException aggregated =
                assertInstanceOf(ProcessorException.class, result.exception());
        assertEquals(Arrays.asList(first, second), aggregated.causes());
        assertTrue(result.isError());
    }

    @Test
    public void foldCombineExceptionBecomesError() {
        IOException merge = new IOException("merge");
        boolean[] later = {false};
        Processor<String, String> list =
                folding(
                        Arrays.asList(
                                input -> done("a"),
                                input -> done("b"),
                                input -> {
                                    later[0] = true;
                                    return done("c");
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
                        Arrays.asList(
                                input -> done("a"),
                                input -> error(disk),
                                input -> done("b"),
                                input -> {
                                    later[0] = true;
                                    return done("c");
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
        assertEquals(Arrays.asList(disk, merge), aggregated.causes());
        assertTrue(later[0]);
    }

    private static <O> Processor<String, O> folding(
            List<? extends Processor<String, O>> processors,
            ThrowableBiFunction<O, O, O> combiner,
            boolean continueOnError) {
        ListProcessor.ListProcessorBuilder<String, O> builder =
                ListProcessor.<String, O>listProcessor().combiner(combiner);
        processors.forEach(builder::processor);
        if (continueOnError) {
            builder.modifier(ProcessorModifier.CONTINUE_ON_ERROR);
        }
        return builder.build();
    }

    private static final class SkippingProcessor implements Processor<String, String> {

        @Override
        public Processed<String> process(String input) {
            return skipped();
        }
    }

    private static final class DoneProcessor implements Processor<String, String> {

        @Override
        public Processed<String> process(String input) {
            return done("polo");
        }
    }
}
