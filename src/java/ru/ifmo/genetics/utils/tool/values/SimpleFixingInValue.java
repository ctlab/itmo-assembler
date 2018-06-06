package ru.ifmo.genetics.utils.tool.values;

public class SimpleFixingInValue<T> implements FixingInValue<T> {
    private final T value;
    public SimpleFixingInValue(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String toString() {
        return "SimpleFixingInValue{" +
                "value=" + value +
                '}';
    }
}
