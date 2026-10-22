package ru.operation.util.function;

@FunctionalInterface
public interface ThrowableRunnable {
    static Runnable wrapExceptions(ThrowableRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    void run() throws Exception;
}
