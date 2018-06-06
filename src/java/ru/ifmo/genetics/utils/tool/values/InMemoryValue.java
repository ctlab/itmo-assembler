package ru.ifmo.genetics.utils.tool.values;

public class InMemoryValue<T> extends InOutValue<T>  {
    T value;

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

}
