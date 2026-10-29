package ru.operation.handler;

import java.util.List;
import lombok.experimental.StandardException;
import ru.operation.OperationException;

@StandardException
public class HandlerException extends OperationException {

    public HandlerException(List<? extends Throwable> causes) {
        super(causes);
    }
}
