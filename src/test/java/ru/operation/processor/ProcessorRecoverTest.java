package ru.operation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.operation.processor.Processed.done;
import static ru.operation.processor.Processed.skipped;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.operation.Failure;
import ru.operation.util.function.ThrowableFunction;

public class ProcessorRecoverTest {

    @Test
    public void recoverTurnsErrorIntoDoneFromTheInput() {
        IllegalStateException boom = new IllegalStateException("down");
        Processor<String, String> recovered =
                Processor.<String, String>of(
                                input -> {
                                    throw boom;
                                })
                        .recover(
                                failure ->
                                        failure.input() + ":" + failure.exception().getMessage());

        Processed<String> result = recovered.process("card");

        assertTrue(result.isDone());
        assertEquals("card:down", result.value());
        assertNull(result.exception());

        Processed<String> empty =
                Processor.<String, String>of(
                                input -> {
                                    throw boom;
                                })
                        .recover(failure -> null)
                        .process("card");
        assertTrue(empty.isDone());
        assertNull(empty.value());

        IllegalArgumentException recoveryBoom = new IllegalArgumentException("nope");
        Processed<String> failedRecovery =
                Processor.<String, String>of(
                                input -> {
                                    throw boom;
                                })
                        .recover(
                                failure -> {
                                    throw recoveryBoom;
                                })
                        .process("card");
        assertTrue(failedRecovery.isError());
        assertSame(recoveryBoom, failedRecovery.exception());
    }

    @Test
    public void recoverLeavesDoneAndSkipped() {
        List<String> seen = new ArrayList<>();
        ThrowableFunction<Failure<String>, String> recovery =
                failure -> {
                    seen.add("recover");
                    return "recovered";
                };

        Processor<String, String> done = input -> done(input);
        assertEquals("ab", done.recover(recovery).process("ab").value());

        Processor<String, String> empty = input -> done(null);
        Processed<String> doneNull = empty.recover(recovery).process("ab");
        assertTrue(doneNull.isDone());
        assertNull(doneNull.value());

        Processor<String, String> skipping = input -> skipped();
        assertTrue(skipping.recover(recovery).process("ab").isSkipped());
        assertTrue(seen.isEmpty());
    }
}
