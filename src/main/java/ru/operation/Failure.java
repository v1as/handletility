package ru.operation;

/** Context of a failed step: original input plus the exception. */
public record Failure<I>(I input, Exception exception) {}
