package ru.ifmo.genetics.structures.arrays;

import org.apache.commons.lang.mutable.MutableLong;

public abstract class Proxy<A extends BigCompoundArray> {
    protected final A backingArray;
    public final MutableLong index = new MutableLong(-1);

    protected Proxy(A array) {
        this.backingArray = array;
    }

    protected IntField intField(BigIntegerArray array) {
        return new IntField(array, index);
    }

}
