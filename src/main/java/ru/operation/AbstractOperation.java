package ru.operation;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

public abstract class AbstractOperation {

    protected Logger log = getLogger(this.getClass());
    private String name = "";
    protected boolean nameDefined = false;

    @Override
    public String toString() {
        return name == null || name.isEmpty() ? this.toString() : this + "[" + name + "]";
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.log = getLogger(this.getClass().getName() + "[" + name + "]");
        this.name = name;
        this.nameDefined = true;
    }

    public static <T extends AbstractOperation> T withName(T operation, String name) {
        operation.setName(name);
        return operation;
    }
}
