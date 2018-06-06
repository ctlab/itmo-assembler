package ru.ifmo.genetics.structures.arrays;

import org.apache.commons.lang.mutable.MutableLong;

public class Field<A> {
    protected final A backingArray;
    protected final MutableLong index;

    public Field(A backingArray, MutableLong index) {
        this.backingArray = backingArray;
        this.index = index;
    }
}
