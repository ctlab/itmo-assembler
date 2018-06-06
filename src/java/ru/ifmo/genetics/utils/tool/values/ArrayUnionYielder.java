package ru.ifmo.genetics.utils.tool.values;

import java.util.ArrayList;
import java.util.Arrays;

public class ArrayUnionYielder<T> extends Yielder<T[]> {
    private final InValue<T[]>[] inValues;

    public ArrayUnionYielder(InValue<T[]>... inValues) {
        this.inValues = inValues;
    }

    @Override
    public T[] yield() {
        ArrayList<T> res = new ArrayList<T>();

        T[] tempArray = null;
        for (InValue<T[]> inValue: inValues) {
            T[] value = inValue.get();
            if (value == null) {
                return null;
            }
            tempArray = value;

            for (T t: value) {
                res.add(t);
            }
        }
        tempArray = (T[]) Arrays.copyOf(tempArray, 0, tempArray.getClass());
//        System.err.println("AUY: " + res);
        return res.toArray(tempArray);
    }

    @Override
    public String description() {
        return null;
    }
}
