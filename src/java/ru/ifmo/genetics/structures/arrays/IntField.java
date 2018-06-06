package ru.ifmo.genetics.structures.arrays;

import org.apache.commons.lang.mutable.MutableLong;

public class IntField extends Field<BigIntegerArray> {
    public IntField(BigIntegerArray backingArray, MutableLong index) {
        super(backingArray, index);
    }

    public int get() {
        return backingArray.get(index.longValue());
    }

    public void set(int value) {
        backingArray.set(index.longValue(), value);
    }
}
