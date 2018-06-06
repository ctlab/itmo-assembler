package ru.ifmo.genetics.tools.olc.arrays;

import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;

import java.io.*;

public class LargeByteArray {
    static final int FIRST_SHIFT = 30;      // 1e9 elements
    static final int SMALL_ARRAY_LEN = 1 << FIRST_SHIFT;
    static final int SECOND_MASK = SMALL_ARRAY_LEN - 1;

    private byte[][] array;
    public final long length;


    public LargeByteArray(long size) {
        length = size;

        int c = (int) (size >> FIRST_SHIFT);
        int r = (int) (size & SECOND_MASK);
        
        array = new byte[c + ((r == 0) ? 0 : 1)][];
        for (int i = 0; i < c; i++) {
            array[i] = new byte[SMALL_ARRAY_LEN];
        }
        if (r != 0) {
            array[c] = new byte[r];
        }
    }
    

    public LargeByteArray(byte[] array) {
        this(array.length);
        for (int i = 0; i < length; i++) {
            set(i, array[i] & 0xff);
        }
    }


    public void save(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        
        for (long i = 0; i < length; ++i) {
            out.write(GluedDnasString.charCodes[get(i)]);
        }
        out.close();
    }
    

    public int get(long pos) {
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        return array[f1][f2] & 0xff;
    }
    
    public int getWithLastZeros(long pos) {
        if (pos >= length) {
            return 0;
        }
        return get(pos);
    }

    public void set(long pos, int value) {
        int f1 = (int) (pos >> FIRST_SHIFT);
        int f2 = (int) (pos & SECOND_MASK);
        array[f1][f2] = (byte)value;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (long i = 0; i < length; i++) {
            if (i != 0) {
                sb.append(", ");
            }

            sb.append(String.format("0x%x", get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
}
