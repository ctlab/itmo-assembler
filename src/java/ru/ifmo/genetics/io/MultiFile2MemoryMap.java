package ru.ifmo.genetics.io;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class MultiFile2MemoryMap {

    static final int ARRAY_SIZE = 1 << 26;
    byte[][] data;
    final long size;
    final RandomAccessMultiFile mf;

    public MultiFile2MemoryMap(String[] files) throws IOException {
        this(FileUtils.convert(files));
    }

    public MultiFile2MemoryMap(File[] files) throws IOException {
        mf = new RandomAccessMultiFile(files, "rw");
        size = mf.size();
        int arraysNumber = (int)(size / ARRAY_SIZE) + 1;
        data = new byte[arraysNumber][];
        long offset = 0;
        for (int i = 0; i < data.length; ++i) {
            long newOffset = Math.min(size, offset + ARRAY_SIZE);
            data[i] = new byte[(int)(newOffset - offset)];
            offset += mf.read(offset, data[i], 0, (int)(newOffset - offset));
        }
    }

    public void dump() throws IOException {
        mf.seek(0);
        for (byte[] ar : data) {
            mf.write(ar);
        }
    }

    public byte readByte(long pos) throws IOException {
        int ind = (int)(pos / ARRAY_SIZE);
        int off = (int)(pos % ARRAY_SIZE);
        return data[ind][off];
    }

    public int read(long pos, byte[] b) throws IOException {
        return read(pos, b, 0, b.length);
    }

    public int read(long pos, byte[] b, int offset, int len) throws IOException {
        int begin = offset;
        int end = begin + len;
        for (int i = begin; i < end; ++i) {
            long cpos = pos + i;
            if (cpos > size) {
                return i;
            }
            b[i] = readByte(cpos);
        }
        return b.length;
    }

    public int readInt(long pos) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        read(pos, bb.array());
        return bb.getInt();
    }

    public long readLong(long pos) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        read(pos, bb.array());
        return bb.getLong();
    }

    public void writeByte(long pos, byte b) throws IOException {
        int ind = (int)(pos / ARRAY_SIZE);
        int off = (int)(pos % ARRAY_SIZE);
        data[ind][off] = b;
    }

    public void write(long pos, byte[] b) throws IOException {
        write(pos, b, 0, b.length);
    }

    public void write(long pos, byte[] b, int offset, int len) throws IOException {
        int begin = offset;
        int end = begin + len;
        for (int i = begin; i < end; ++i) {
            long cpos = pos + i;
            if (cpos > size) {
                return;
            }
            writeByte(cpos, b[i]);
        }
    }

    public void writeInt(long pos, int x) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(x);
        write(pos, bb.array());
    }

    public void writeLong(long pos, long x) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(x);
        write(pos, bb.array());
    }

    public void writeDnaQ(long pos, DnaQ d) throws IOException {
        int oldLen = readInt(pos);
        int newLen = d.length();
        writeInt(pos, newLen);
        write(pos + 4, d.toByteArray());
        for (int i = newLen; i < oldLen; ++i) {
            writeByte(pos + 4 + i, (byte)-1);
        }
    }

}
