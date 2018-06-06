package ru.ifmo.genetics.utils.tool.values;

public class ForwardingFixingInValue<T> implements FixingInValue<T> {
    private InValue<T> value;

    public ForwardingFixingInValue(InValue<T> value) {
        this.value = value;
    }

    @Override
    public T get() {
        if (value == null) {
            return null;
        }
        return value.get();
    }

    @Override
    public String toString() {
        return "ForwardingFixingInValue{" +
                "value=" + value +
                '}';
    }
}
