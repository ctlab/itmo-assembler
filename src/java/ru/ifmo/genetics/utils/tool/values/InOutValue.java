package ru.ifmo.genetics.utils.tool.values;

public abstract class InOutValue<T> implements InValue<T>,  OutValue<T> {
    public InValue<T> inValue() {
        return this;
    }

    public OutValue<T> outValue() {
        return this;
    }
}
