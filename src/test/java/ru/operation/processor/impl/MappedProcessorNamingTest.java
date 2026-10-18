package ru.operation.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MappedProcessorNamingTest {

    @Test
    public void setNamePropagatesToMapperAndDelegate() {
        var mapper = new SimpleProcessor<String, Integer>(Integer::valueOf);
        var delegate = new SimpleProcessor<Integer, Integer>(n -> n * 2);
        var processor = new MappedProcessor<>(mapper, delegate);

        processor.setName("double");

        assertEquals("double", processor.getName());
        assertEquals("double-mapper", mapper.getName());
        assertEquals("double-delegate", delegate.getName());
    }
}
