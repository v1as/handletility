package ru.operation;

/** Failed operation context: original input plus the exception. */
public record Failed<I>(I value, Exception exception) {}
