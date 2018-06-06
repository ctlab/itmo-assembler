package ru.ifmo.genetics.tools.io;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.*;

public class LazyLongReader {

    public static final int DEFAULT_BUFFER_SIZE = 1 << 23;

    private MultipleFilesByteArrayReader reader;
    ByteBuffer bb = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    int size = 0;

    boolean eof = false;

    public LazyLongReader(String[] files) throws EOFException, FileNotFoundException {
        reader = new MultipleFilesByteArrayReader(files);
    }

    public LazyLongReader(String file) throws EOFException, FileNotFoundException {
        this(new String[]{file});
    }

    public LazyLongReader(File[] files) throws EOFException, FileNotFoundException {
        reader = new MultipleFilesByteArrayReader(files);
    }

    public long readLong() throws IOException {
        if (eof) {
            throw new EOFException();
        }
        if (bb.position() == size) {
            bb.clear();
            size = reader.read(bb.array());
            if (size == -1) {
                eof = true;
                throw new EOFException();
            }
        }
        return bb.getLong();
    }

    public byte[] read() throws IOException {
        if (eof) {
            throw new EOFException();
        }
        if (bb.position() == size) {
            bb.clear();
            size = reader.read(bb.array());
            if (size == -1) {
                eof = true;
                throw new EOFException();
            }
        }
        byte[] ar = new byte[size - bb.position()];
        bb.get(ar);
        return ar;
    }

}
