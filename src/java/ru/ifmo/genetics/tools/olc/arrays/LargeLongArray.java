package ru.ifmo.genetics.tools.olc.arrays;


public class LargeLongArray {
    static final int FIRST_SHIFT = 30;      // 1e9 elements
    static final int SMALL_ARRAY_LEN = 1 << FIRST_SHIFT;
    static final int SECOND_MASK = SMALL_ARRAY_LEN - 1;

    private long[][] array;
    public final long length;


    public LargeLongArray(long size) {
        length = size;

        int c = (int) (size >> FIRST_SHIFT);
        int r = (int) (size & SECOND_MASK);

        array = new long[c + ((r == 0) ? 0 : 1)][];
        for (int i = 0; i < c; i++) {
            array[i] = new long[SMALL_ARRAY_LEN];
        }
        if (r != 0) {
            array[c] = new long[r];
        }
    }

    public long get(long pos) {
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        return array[f1][f2];
    }
    
    public long getWithLastZeros(long pos) {
        if (pos >= length) {
            return 0;
        }
        return get(pos);
    }

    public void set(long pos, long value) {
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        array[f1][f2] = value;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (long i = 0; i < length; i++) {
            if (i != 0) {
                sb.append(", ");
            }

            sb.append(get(i));
        }
        sb.append("]");
        return sb.toString();
    }
    
}
