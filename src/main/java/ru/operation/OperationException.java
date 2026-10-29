package ru.operation;

import java.util.List;

public class OperationException extends RuntimeException {

    private final List<Throwable> causes;

    public OperationException(Throwable cause) {
        super(cause);
        this.causes = cause == null ? List.of() : List.of(cause);
    }

    public OperationException(String message) {
        super(message);
        this.causes = List.of();
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
        this.causes = cause == null ? List.of() : List.of(cause);
    }

    protected OperationException(List<? extends Throwable> causes) {
        super(describe(causes), causes.get(0));
        this.causes = List.copyOf(causes);
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
            if (!message.isEmpty()) {
                message.append("; ");
            }
            message.append(cause.getMessage());
        }
        return message.isEmpty() ? null : message.toString();
    }
}
