package ru.operation;

/** Failed operation context: original input plus the exception. */
public record Failure<I>(I input, Exception exception) {}
