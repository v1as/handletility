package ru.v1as;

public record Failed<I>(I value, Exception exception) {}
