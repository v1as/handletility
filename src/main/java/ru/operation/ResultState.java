package ru.operation;

/** Outcome of a handler or processor invocation. */
public enum ResultState {
    /** Input was not applicable or no work was done. */
    SKIPPED,
    /** Work completed successfully. */
    DONE,
    /** Work failed; see accompanying exception when present. */
    ERROR
}
