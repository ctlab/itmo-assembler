package ru.ifmo.genetics.utils.tool.values;

public abstract class FixingYielder<T> implements FixingInValue<T>{

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
