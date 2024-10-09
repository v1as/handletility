package ru.v1as.processor.impl.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.v1as.processor.Processor;
import ru.v1as.processor.impl.IdentifiedByConstProcessor;
import ru.v1as.processor.impl.SimpleProcessor;

public class ProcessorMapTest {

    public static final IdentifiedByConstProcessor<String, Integer, Integer> ADD_PROCESSOR =
            new IdentifiedByConstProcessor<>("ADD", new SimpleProcessor<>(value -> ++value));

    public static final Processor<Integer, Integer> ADD_TEN_PROCESSOR =
            new SimpleProcessor<>(value -> value + 10);

    public static final IdentifiedByConstProcessor<String, Integer, Integer> SUB_PROCESSOR =
            new IdentifiedByConstProcessor<>("SUB", new SimpleProcessor<>(value -> --value));

    public static final SimpleProcessor<Integer, String> KEY_PROCESSOR =
            new SimpleProcessor<>(
                    value -> {
                        if (value > 0) {
                            return "ADD";
                        } else if (value < 0) {
                            return "SUB";
                        }
                        return "ZERO";
                    });

    @Test
    public void shouldProcessWithoutDefaultProcessor() {
        ProcessorMap<String, Integer, Integer> processor =
                new ProcessorMap<>(
                        List.of(ADD_PROCESSOR, SUB_PROCESSOR), KEY_PROCESSOR, ADD_TEN_PROCESSOR);
        assertEquals(11, processor.process(10).value());
        assertEquals(-11, processor.process(-10).value());
        assertEquals(10, processor.process(0).value());
    }

    @Test
    public void shouldProcessWithDefaultProcessor() {}
}
