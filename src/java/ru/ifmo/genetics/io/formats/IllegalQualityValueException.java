package ru.ifmo.genetics.io.formats;

public class IllegalQualityValueException extends RuntimeException {
    public IllegalQualityValueException() {
    }

    public IllegalQualityValueException(String message) {
        super(message);
    }

    public IllegalQualityValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalQualityValueException(Throwable cause) {
        super(cause);
    }
}
