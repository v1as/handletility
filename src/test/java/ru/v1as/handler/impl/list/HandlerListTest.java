package ru.v1as.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.v1as.handler.Handled.handled;
import static ru.v1as.handler.Handled.skipped;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.v1as.handler.AbstractHandler;
import ru.v1as.handler.Handled;
import ru.v1as.handler.Handler;
import ru.v1as.handler.IdentifiedHandler;
import ru.v1as.handler.impl.CompositeHandler;
import ru.v1as.handler.impl.SimpleHandler;
import ru.v1as.handler.impl.map.HandlerMap;
import ru.v1as.processor.impl.SimpleProcessor;

public class HandlerListTest {

    @Test
    public void should_process_after_skip() {
        Handler<String> handlers =
                new HandlerList<>(List.of(value -> skipped(), value -> handled()));
        Handled result = handlers.handle("hello");
        assertEquals(handled(), result);
    }

    @Test
    public void testProgram() {
        Handler<Command> baseHandler =
                CompositeHandler.<Command>builder()
                        .beforeHandler(new LoggingHandler())
                        .handler(
                                HandlerList.<Command>builder()
                                        .handler(
                                                HandlerMap.<String, Command>handlerMap()
                                                        .handler(new HelloCommand())
                                                        .handler(new ByeCommand())
                                                        .keyExtractor(
                                                                new SimpleProcessor<>(
                                                                        Command::name))
                                                        .build())
                                        .handler(
                                                new SimpleHandler<>(
                                                        Objects::nonNull, System.out::println))
                                        .build())
                        .build();

        System.out.println(baseHandler.handle(new Command("hello", null)));
        System.out.println(baseHandler.handle(new Command("bye", null)));
        System.out.println(baseHandler.handle(new Command("some", null)));
    }

    private record Command(String name, List<String> argument) {}

    @Slf4j
    private static class LoggingHandler implements Handler<Command> {

        @Override
        public Handled handle(Command input) {
            log.info("Command received {}", input);
            return handled();
        }
    }

    @RequiredArgsConstructor
    private abstract static class AbstractCommand extends AbstractHandler<Command>
            implements IdentifiedHandler<String, Command> {

        private final String name;

        @Override
        public String getIdentifier() {
            return name;
        }
    }

    private static class HelloCommand extends AbstractCommand {

        public HelloCommand() {
            super("hello");
        }

        @Override
        protected Handled handleInternal(Command input) {
            System.out.println("Hello " + input);
            return handled();
        }
    }

    private static class ByeCommand extends AbstractCommand {

        public ByeCommand() {
            super("bye");
        }

        @Override
        protected Handled handleInternal(Command input) {
            System.out.println("Bye " + input);
            return handled();
        }
    }
}
