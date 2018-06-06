package ru.ifmo.genetics.utils.tool.values;

public abstract class Yielder<T> implements InValue<T> {

    public abstract T yield();
    public abstract String description();

    @Override
    public T get() {
        return yield();
    }

    @Override
    public String toString() {
        return description();
    }

}
