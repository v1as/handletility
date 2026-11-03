package ru.operation.handler.impl.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.operation.handler.Handled.done;
import static ru.operation.handler.Handled.skipped;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.operation.handler.AbstractHandler;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;
import ru.operation.handler.IdentifiedHandler;
import ru.operation.handler.impl.AroundHandler;
import ru.operation.handler.impl.routing.RoutingHandler;
import ru.operation.processor.Processor;
import ru.operation.processor.impl.FunctionProcessor;

public class HandlerListTest {

    @Test
    public void should_process_after_skip() {
        Handler<String> handlers = new HandlerList<>(List.of(value -> skipped(), value -> done()));
        Handled result = handlers.handle("hello");
        assertEquals(done(), result);
    }

    @Test
    public void testProgram() {
        Handler<Command> baseHandler =
                AroundHandler.<Command>builder()
                        .beforeHandler(new LoggingHandler())
                        .delegate(
                                HandlerList.<Command>builder()
                                        .handler(
                                                RoutingHandler.<String, Command>routingHandler()
                                                        .handler(new HelloCommand())
                                                        .handler(new ByeCommand())
                                                        .identifierExtractor(
                                                                new FunctionProcessor<>(
                                                                        Command::name))
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

    private record Command(String name, List<String> argument) {}

    @Slf4j
    private static class LoggingHandler implements Handler<Command> {

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
}
