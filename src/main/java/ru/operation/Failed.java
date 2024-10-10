package ru.operation;

public record Failed<I>(I value, Exception exception) {}
