package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class HandlerOrTest {

    @Test
    public void orTriesNextOnlyOnSkip() {
        List<String> seen = new ArrayList<>();
        Handler<String> first =
                input -> {
                    seen.add("first");
                    return skipped();
                };
        Handler<String> second =
                input -> {
                    seen.add("second");
                    return done();
                };
        Handler<String> third =
                input -> {
                    seen.add("third");
                    return done();
                };

        Handled result = first.or(second).or(third).handle("marko");

        assertTrue(result.isDone());
        assertEquals(Arrays.asList("first", "second"), seen);
    }

    @Test
    public void orStopsOnDoneAndError() {
        List<String> seen = new ArrayList<>();
        Handler<String> next =
                input -> {
                    seen.add("next");
                    return done();
                };

        Handler<String> done = input -> done();
        assertTrue(done.or(next).handle("x").isDone());
        assertTrue(seen.isEmpty());

        Handler<String> failing = input -> error(new IllegalStateException("boom"));
        Handled error = failing.or(next).handle("x");
        assertTrue(error.isError());
        assertEquals("boom", error.exception().getMessage());
        assertTrue(seen.isEmpty());
    }

    @Test
    public void identifiedByBindsIdentifierAndDelegates() {
        List<String> seen = new ArrayList<>();
        IdentifiedHandler<String, String> identified =
                Handler.<String>of(seen::add).identifiedBy("audit");

        assertEquals("audit", identified.identifier());
        assertTrue(identified.handle("marko").isDone());
        assertEquals(Arrays.asList("marko"), seen);
    }

    @Test
    public void orStaysSkippedWhenEveryStepSkips() {
        Handler<String> skipping = input -> skipped();

        assertTrue(skipping.or(skipping).handle("x").isSkipped());
    }

    @Test
    public void orFlattensToSingleOrHandler() {
        Handler<String> a = new Step(true);
        Handler<String> b = new Step(true);
        Handler<String> c = new Step(false);

        Handler<String> chain = a.or(b).or(c);

        assertEquals(OrHandler.class, chain.getClass());
        assertEquals(
                Arrays.asList(
                        "Handling input 'x'",
                        "Handler 'Step' has result '[SKIPPED]'",
                        "Handler 'Step' has result '[SKIPPED]'",
                        "Handler 'Step' has result '[DONE]'",
                        "Handled input 'x' with result [DONE]"),
                traceMessages(chain, "x"));
    }

    private static final class Step implements Handler<String> {

        private final boolean skip;

        Step(boolean skip) {
            this.skip = skip;
        }

        @Override
        public Handled handle(String input) {
            return skip ? skipped() : done();
        }
    }

    private static List<String> traceMessages(Handler<String> chain, String input) {
        Logger logger = (Logger) LoggerFactory.getLogger(OrHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            chain.handle(input);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
