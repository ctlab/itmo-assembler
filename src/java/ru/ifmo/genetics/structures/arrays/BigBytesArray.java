package ru.ifmo.genetics.structures.arrays;

public class BigBytesArray {

    private final static int BUCKET_SIZE_POWER = 30;
    private final static int BUCKET_SIZE = 1 << BUCKET_SIZE_POWER;
    private final static int BUCKET_SIZE_MASK = BUCKET_SIZE - 1;

    private byte[][] ar;

    public BigBytesArray(long size) {
        ar = new byte[(int)((size - 1) / BUCKET_SIZE + 1)][BUCKET_SIZE];
    }

    public void set(long i, byte value) {
        ar[(int)(i >>> BUCKET_SIZE_POWER)][(int)(i & BUCKET_SIZE_MASK)] = value;
    }

    public byte get(long i) {
        return ar[(int)(i >>> BUCKET_SIZE_POWER)][(int)(i & BUCKET_SIZE_MASK)];
    }
}
