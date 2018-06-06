package ru.ifmo.genetics.tools.olc.arrays;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static ru.ifmo.genetics.tools.olc.arrays.Util.read5ByteFromStream;
import static ru.ifmo.genetics.tools.olc.arrays.Util.write5ByteToStream;

public class Large5ByteArray {
    // TODO use one big array
    private static final long FIRST_SHIFT = 24;
    private static final int SMALL_ARRAY_LEN = 1 << FIRST_SHIFT;
    private static final long SECOND_MASK = SMALL_ARRAY_LEN - 1;
    
    private static final long MAX_VALUE = 1L << 40;
    private static final long HIGH_MASK = 0xFFL;
    private static final long LOW_MASK = 0xFFFFFFFFL;

    private int[][] arrayLow;
    private byte[][] arrayHigh;
    public final long length;

    /***
     * Load array from 5b format
     */
    public Large5ByteArray(InputStream is) throws IOException {
        length = read5ByteFromStream(is);
        initArray(length);
        long xor = 0;
        for (long i = 0; i < length; i++) {
            long value = read5ByteFromStream(is);
            xor ^= value;
            set(i, value);
        }
        long checksum = read5ByteFromStream(is);
        if (xor != checksum) {
            throw new UnsupportedEncodingException();
        }
    }

    /***
     * Save array to 5b format
     */
    public void save(OutputStream os) throws IOException {
        write5ByteToStream(os, length);
        long xor = 0;
        for (long i = 0; i < length; i++) {
            long value = get(i);
            xor ^= value;
            write5ByteToStream(os, value);
        }
        write5ByteToStream(os, xor);
    }

    public Large5ByteArray(long size) {
        length = size;
        initArray(size);
    }

    private void initArray(long size) {
        int f1 = (int) (size >> FIRST_SHIFT) + 1;
        int f2 = (int) (SECOND_MASK) + 1;
        arrayHigh = new byte[f1][f2];
        arrayLow = new int[f1][f2];
        
        int c = (int) (size / SMALL_ARRAY_LEN);
        int r = (int) (size % SMALL_ARRAY_LEN);
        
        arrayHigh = new byte[c + 1][];
        arrayLow = new int[c + 1][];
        for (int i = 0; i < c; i++) {
            arrayHigh[i] = new byte[SMALL_ARRAY_LEN];
            arrayLow[i] = new int[SMALL_ARRAY_LEN];
        }
        arrayHigh[c] = new byte[r];
        arrayLow[c] = new int[r];
    }

    public long get(long pos) {
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        long higherByte = ((long) arrayHigh[f1][f2]) & HIGH_MASK;
        long lowerInt = ((long) arrayLow[f1][f2]) & LOW_MASK;
        return (higherByte << 32L) | lowerInt;
    }

    public void set(long pos, long value) {
//        if (value < 0 || value >= MAX_VALUE) {
//            throw new AssertionError("value" + value + " unsupported");
//        }
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        arrayHigh[f1][f2] = (byte) (value >> 32);
        arrayLow[f1][f2] = (int) (value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (long i = 0; i < length; i++) {
            sb.append(get(i));
            if (i < length - 1) {
                sb.append(", ");
            } else {
                sb.append("]");
            }

        }
        return sb.toString();
    }

}
