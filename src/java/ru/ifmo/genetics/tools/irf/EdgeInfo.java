package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.structures.arrays.BigCompoundArray;
import ru.ifmo.genetics.structures.arrays.BigIntegerArray;
import ru.ifmo.genetics.structures.arrays.IntField;
import ru.ifmo.genetics.structures.arrays.Proxy;

public class EdgeInfo extends Proxy<EdgeInfo.Array> {
    public final IntField weight = intField(backingArray.weight) ;


    protected EdgeInfo(Array array, long index) {
        super(array);
        this.index.setValue(index);
    }

    public static class Array extends BigCompoundArray<EdgeInfo> {
        BigIntegerArray weight = intArray();

        public Array(long size) {
            super(size);
        }

        @Override
        public EdgeInfo get(long index) {
            return new EdgeInfo(this, index);
        }
    }
}
