package ru.ifmo.genetics.utils.tool.values;

public class ToStringYielder<T> extends Yielder<String> {
    private final InValue<T> inValue;

    public ToStringYielder(InValue<T> inValue) {
        this.inValue = inValue;
    }

    public static <T> ToStringYielder<T> create(InValue<T> inValue) {
        return new ToStringYielder<T>(inValue);
    }


    @Override
    public String yield() {
        return inValue.get().toString();
    }

    @Override
    public String description() {
        return "converts to string";
    }
}
