package ru.ifmo.genetics.utils.tool.values;

public class MaxYielder<T extends Comparable<T>> extends Yielder<T> {
    private final InValue<T> a;
    private final InValue<T> b;

    public MaxYielder(InValue<T> a, InValue<T> b) {
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
            return bValue;
        }
        return aValue;
    }

    @Override
    public String description() {
        return "maximal value from " + a + " and " + b;
    }
}
