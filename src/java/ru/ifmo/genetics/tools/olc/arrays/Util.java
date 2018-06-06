package ru.ifmo.genetics.tools.olc.arrays;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
    public static void write5ByteToStream(OutputStream os, long value) throws IOException {
        for (int j = 4; j >= 0; j--) {
            os.write((byte) (value >> (8 * j)));
        }
    }

    public static long read5ByteFromStream(InputStream is) throws IOException {
        long res = 0;
        for (int j = 4; j >= 0; j--) {
            int x = is.read();
            if (x == -1)
                throw new EOFException();
            res |= ((long) x) << (8 * j);
        }
        return res;
    }
}
