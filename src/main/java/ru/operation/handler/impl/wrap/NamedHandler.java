package ru.operation.handler.impl.wrap;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.operation.handler.Handled;
import ru.operation.handler.Handler;

/** Decorator that assigns a display name to a handler (for logging / toString). */
@RequiredArgsConstructor
public class NamedHandler<I> implements Handler<I> {

    @NonNull @Getter private final String name;
    @NonNull private final Handler<I> delegate;

    @Override
    public Handled handle(I input) {
        return delegate.handle(input);
    }

    @Override
    public String toString() {
        return "Named[" + name + "](" + delegate + ")";
    }
}
