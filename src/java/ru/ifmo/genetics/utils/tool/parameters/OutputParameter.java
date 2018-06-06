package ru.ifmo.genetics.utils.tool.parameters;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.values.InOutValue;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.SimpleInValue;

public class OutputParameter<T> extends InOutValue<T> {
    public final @NotNull Class<T> tClass;
    public final @NotNull String name;
    private @NotNull InValue<T> value;

    public OutputParameter(@NotNull String name, @NotNull InValue<T> value, @NotNull Class<T> tClass) {
        this.name = name;
        this.value = value;
        this.tClass = tClass;
    }


    @Override
    public T get() {
//        if (value == null) {
//            return null;
//        }
        return value.get();
    }

    @Override
    public void set(T value) {
        this.value = new SimpleInValue<T>(value);
    }

    @Override
    public String toString() {
        return "OutputParameter{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
