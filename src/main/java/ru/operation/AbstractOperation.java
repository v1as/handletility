package ru.operation;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/** Base for named operations with an SLF4J logger. */
public abstract class AbstractOperation {

    protected Logger log = getLogger(this.getClass());
    private String name = "";
    protected boolean nameDefined = false;

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        return name.isEmpty() ? className : className + "[" + name + "]";
    }

    /** Sets a display name and scopes the logger as {@code ClassName[name]}. */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.log = getLogger(this.getClass().getName() + "[" + name + "]");
        this.name = name;
        this.nameDefined = true;
    }

    public String getName() {
        return name;
    }

    public static <T extends AbstractOperation> T withName(T operation, String name) {
        operation.setName(name);
        return operation;
    }
}
