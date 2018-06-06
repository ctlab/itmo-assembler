package ru.ifmo.genetics.structures.arrays;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BigIntegerArray implements BigArray {
    public static final int logSmallCapacity = 20;    // 1 M  [ * 8 byte = 8 Mb]
    public static final int smallCapacity = 1 << logSmallCapacity;
    public static final int smallCapacityMask = smallCapacity - 1;

    private int[][] ar;
    private long size;

    public BigIntegerArray() {
        this(0);
    }

    public BigIntegerArray(long size) {
        reset(size);
    }

    public void reset(long size) {
        this.size = size;
        int cnt = (int)((size + smallCapacity - 1) >>> logSmallCapacity);
        ar = new int[cnt][smallCapacity];
    }

    public int get(long i) {
        assert 0 <= i && i < size : i;

        return ar[(int)(i >>> logSmallCapacity)][(int)(i & smallCapacityMask)];
    }

    public int set(long i, int value) {
        assert 0 <= i && i < size : i;

        int i1 = (int)(i >>> logSmallCapacity);
        int i2 = (int)(i & smallCapacityMask);
        int old = ar[i1][i2];
        ar[i1][i2] = value;
        return old;
    }

    public long size() {
        return size;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(size);
        for (long i = 0; i < size; ++i) {
            dataOutput.writeInt(get(i));
        }
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        size = dataInput.readLong();
        reset(size);
        for (long i = 0; i < size; ++i) {
            set(i, dataInput.readInt());
        }
    }
}
