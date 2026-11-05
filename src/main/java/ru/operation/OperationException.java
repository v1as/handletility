package ru.operation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OperationException extends RuntimeException {

    private final List<Throwable> causes;

    public OperationException(Throwable cause) {
        super(cause);
        this.causes = cause == null ? emptyList() : singletonList(cause);
    }

    public OperationException(String message) {
        super(message);
        this.causes = emptyList();
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
        this.causes = cause == null ? emptyList() : singletonList(cause);
    }

    protected OperationException(List<? extends Throwable> causes) {
        super(describe(causes), causes.get(0));
        this.causes = Collections.unmodifiableList(new ArrayList<>(causes));
    }

    /** Причины этой ошибки: все исходные исключения сводной ошибки либо единственный cause. */
    public List<Throwable> causes() {
        return causes;
    }

    private static String describe(List<? extends Throwable> causes) {
        StringBuilder message = new StringBuilder();
        for (Throwable cause : causes) {
            if (cause.getMessage() == null) {
                continue;
            }
            if (message.length() > 0) {
                message.append("; ");
            }
            message.append(cause.getMessage());
        }
        return message.length() == 0 ? null : message.toString();
    }
}
