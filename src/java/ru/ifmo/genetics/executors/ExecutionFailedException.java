package ru.ifmo.genetics.executors;

public class ExecutionFailedException extends RuntimeException {
    public ExecutionFailedException(String message) {
        super(message);
    }
}
