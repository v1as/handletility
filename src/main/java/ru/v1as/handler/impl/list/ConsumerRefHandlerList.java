package ru.v1as.handler.impl.list;

import static ru.v1as.handler.impl.list.HandlerModifier.CONTINUE_ON_DONE;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import ru.v1as.Failed;
import ru.v1as.handler.Handler;
import ru.v1as.handler.impl.LoggingExceptionHandler;
import ru.v1as.handler.impl.SimpleHandler;

// todo looks to difficult, to remove?
public class ConsumerRefHandlerList<I> extends HandlerList<I> {

    public <T> ConsumerRefHandlerList(
            List<T> items,
            BiConsumer<T, I> method,
            Set<HandlerModifier> modifiers,
            Handler<Failed<I>> exceptionHandler) {
        super(
                items.stream()
                        .map(item -> new SimpleHandler<I>(c -> method.accept(item, c)))
                        .toList(),
                modifiers,
                exceptionHandler);
    }

    public <T> ConsumerRefHandlerList(List<T> items, BiConsumer<T, I> method) {
        this(items, method, Set.of(CONTINUE_ON_DONE), new LoggingExceptionHandler<>());
    }
}
