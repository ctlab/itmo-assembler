package ru.ifmo.genetics.utils.tool.values;

import ru.ifmo.genetics.utils.TextUtils;

public class ArrayToStringYielder<T> extends Yielder<String> {
    private final InValue<T[]> array;
    public final String separator;

    public ArrayToStringYielder(InValue<T[]> array, String separator) {
        this.array = array;
        this.separator = separator;
    }

    public ArrayToStringYielder(InValue<T[]> array) {
        this(array, " ");
    }

    @Override
    public String yield() {
        return TextUtils.arrayToString(array.get(), separator);
    }

    @Override
    public String description() {
        return "converts array to string";
    }
}
