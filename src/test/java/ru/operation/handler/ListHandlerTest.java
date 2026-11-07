package ru.operation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.error;
import static ru.operation.handler.Handled.skipped;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.operation.processor.Processor;

public class ListHandlerTest {

    @Test
    public void should_process_after_skip() {
        Handler<String> handlers =
                new ListHandler<>(Arrays.asList(value -> skipped(), value -> done()));
        Handled result = handlers.handle("hello");
        assertEquals(done(), result);
    }

    @Test
    public void stepTraceIncludesHandlerInputAndResult() {
        Logger logger = (Logger) LoggerFactory.getLogger(ListHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            new ListHandler<>(Arrays.asList(new SkippingHandler(), new DoneHandler()))
                    .handle("hello");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(
                Arrays.asList(
                        "Handling input 'hello'",
                        "Handler 'SkippingHandler' has result '[SKIPPED]'",
                        "Handler 'DoneHandler' has result '[DONE]'",
                        "Handled input 'hello' with result [DONE]"),
                appender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList()));
    }

    @Test
    public void thrownStepIsLoggedAtWarn() {
        IllegalStateException boom = new IllegalStateException("boom");
        Logger logger = (Logger) LoggerFactory.getLogger(ListHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            Handled result = new ListHandler<>(Arrays.asList(new BoomHandler(boom))).handle("x");
            assertSame(boom, result.exception());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertEquals("Handler 'BoomHandler' threw", appender.list.get(0).getFormattedMessage());
        assertEquals("boom", appender.list.get(0).getThrowableProxy().getMessage());
    }

    @Test
    public void returnedErrorIsNotLogged() {
        Logger logger = (Logger) LoggerFactory.getLogger(ListHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            assertTrue(
                    new ListHandler<>(
                                    Arrays.asList(
                                            input -> error(new IllegalStateException("boom"))))
                            .handle("x")
                            .isError());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }

        assertTrue(appender.list.isEmpty());
    }

    @Test
    public void testProgram() {
        Handler<Command> baseHandler =
                AroundHandler.<Command>builder()
                        .beforeHandler(new TraceCommandHandler())
                        .delegate(
                                ListHandler.<Command>builder()
                                        .handler(
                                                SwitchHandler.<String, Command>switchHandler()
                                                        .handler(new HelloCommand())
                                                        .handler(new ByeCommand())
                                                        .keyReader(Processor.of(Command::name))
                                                        .build())
                                        .handler(
                                                Processor.<Command>when(Objects::nonNull)
                                                        .thenHandle(
                                                                Handler.of(System.out::println)))
                                        .build())
                        .build();

        System.out.println(baseHandler.handle(new Command("hello", null)));
        System.out.println(baseHandler.handle(new Command("bye", null)));
        System.out.println(baseHandler.handle(new Command("some", null)));
    }

    private static final class Command {
        private final String name;
        private final List<String> argument;

        private Command(String name, List<String> argument) {
            this.name = name;
            this.argument = argument;
        }

        String name() {
            return name;
        }

        List<String> argument() {
            return argument;
        }
    }

    @Slf4j
    private static class TraceCommandHandler implements Handler<Command> {

        @Override
        public Handled handle(Command input) {
            log.info("Command received {}", input);
            return done();
        }
    }

    private abstract static class AbstractCommand extends AbstractHandler<Command>
            implements IdentifiedHandler<String, Command> {

        private final String identifier;

        protected AbstractCommand(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String identifier() {
            return identifier;
        }
    }

    private static class HelloCommand extends AbstractCommand {

        public HelloCommand() {
            super("hello");
        }

        @Override
        protected Handled handleInternal(Command input) {
            System.out.println("Hello " + input);
            return done();
        }
    }

    private static class ByeCommand extends AbstractCommand {

        public ByeCommand() {
            super("bye");
        }

        @Override
        protected Handled handleInternal(Command input) {
            System.out.println("Bye " + input);
            return done();
        }
    }

    private static final class BoomHandler implements Handler<String> {

        private final RuntimeException exception;

        private BoomHandler(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public Handled handle(String input) {
            throw exception;
        }
    }

    private static final class SkippingHandler implements Handler<String> {

        @Override
        public Handled handle(String input) {
            return skipped();
        }
    }

    private static final class DoneHandler implements Handler<String> {

        @Override
        public Handled handle(String input) {
            return done();
        }
    }
}
