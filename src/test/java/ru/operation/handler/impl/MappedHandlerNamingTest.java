package ru.operation.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.operation.processor.impl.SimpleProcessor;

public class MappedHandlerNamingTest {

    @Test
    public void setNamePropagatesToMapperAndDelegate() {
        var mapper = new SimpleProcessor<String, Integer>(Integer::valueOf);
        var delegate = new SimpleHandler<Integer>(n -> {});
        var handler = new MappedHandler<>(mapper, delegate);

        handler.setName("sum");

        assertEquals("sum", handler.getName());
        assertEquals("sum-mapper", mapper.getName());
        assertEquals("sum-delegate", delegate.getName());
    }
}
