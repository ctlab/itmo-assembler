package ru.ifmo.genetics.utils.tool.values;

public class MinYielder<T extends Comparable<T>> extends Yielder<T> {
    private final InValue<T> a;
    private final InValue<T> b;

    public MinYielder(InValue<T> a, InValue<T> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public T yield() {
        T aValue = a.get();
        T bValue = b.get();
        if (aValue == null || bValue == null) {
            return null;
        }
        if (aValue.compareTo(bValue) <= 0) {
            return aValue;
        }
        return bValue;
    }

    @Override
    public String description() {
        return "minimal value from " + a + " and " + b;
    }
}
