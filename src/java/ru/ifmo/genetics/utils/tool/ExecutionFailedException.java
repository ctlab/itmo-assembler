package ru.ifmo.genetics.utils.tool;

public class ExecutionFailedException extends Exception {
    public ExecutionFailedException(String message) {
        super(message);
    }

    public ExecutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionFailedException(Throwable cause) {
        super(cause);
    }
}
