package ru.ifmo.genetics.structures.arrays;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

public abstract class BigCompoundArray<P> implements BigArray {
    protected final long size;

    protected final ArrayList<BigArray> arrays = new ArrayList<BigArray>();

    protected int elementSizeBytes = 0;

    public BigCompoundArray() {
        this(0);
    }

    public BigCompoundArray(long size) {
        this.size = size;
    }

    public abstract P get(long index);

    @Override
    public void reset(long newSize) {
        for (BigArray array: arrays) {
            array.reset(newSize);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        for (Writable array: arrays) {
            array.write(dataOutput);
        }
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        for (Writable array: arrays) {
            array.readFields(dataInput);
        }
    }

    public BigIntegerArray intArray() {
        BigIntegerArray res = new BigIntegerArray(size);
        elementSizeBytes += 4;
        arrays.add(res);
        return res;
    }

    public int elementSizeBytes() {
        return elementSizeBytes;
    }
}
