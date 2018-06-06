package ru.ifmo.genetics.io;

import java.io.IOException;
import java.io.OutputStream;

public class IOUtils {

    public static void putByteArray(byte[] array, OutputStream out) throws IOException {
        putInt(array.length, out);
        out.write(array);
    }

    public static void putInt(int v, OutputStream out) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 0) & 0xFF);
    }

}
