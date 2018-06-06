package ru.ifmo.genetics.utils.tool.values;

import java.util.Arrays;

public class SimpleInValue<T> implements InValue<T> {
    private final T value;
    public SimpleInValue(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String toString() {
        if (value instanceof Object[]) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }
}
