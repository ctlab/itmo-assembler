package ru.ifmo.genetics.io;

import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.dna.DnaQ;

import java.io.*;

public class RandomAccessMultiFile {

    RandomAccessFile[] files;
    long[] totalSize;
    int ind = 0;

    public RandomAccessMultiFile(String[] files, String mode) throws IOException {
        this(FileUtils.convert(files), mode);
    }

    public RandomAccessMultiFile(File[] files, String mode) throws IOException {
        this.files = new RandomAccessFile[files.length];
        totalSize = new long[files.length];
        long size = 0;
        for (int i = 0; i < files.length; ++i) {
            this.files[i] = new RandomAccessFile(files[i], mode);
            size += this.files[i].length();
            totalSize[i] = size;
        }
    }

    public long size() {
        return totalSize[totalSize.length - 1];
    }

    public long getPos() throws IOException {
        long curPos = files[ind].getFilePointer();
        if (ind > 0) {
            curPos += totalSize[ind - 1];
        }
        return curPos;
    }

    public synchronized void seek(long pos) throws IOException {
        for (ind = 0; totalSize[ind] <= pos; ++ind);
        long cpos = (ind == 0) ? pos : (pos - totalSize[ind - 1]);
        files[ind].seek(cpos);
    }

    public byte readByte() throws IOException {
        if (ind == files.length) {
            throw new EOFException();
        }
        return files[ind].readByte();

    }

    public synchronized byte readByte(long pos) throws IOException {
        seek(pos);
        return readByte();
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public synchronized int read(long pos, byte[] b) throws IOException {
        return read(pos, b, 0, b.length);
    }

    public int read(byte[] b, int offset, int len) throws IOException {
        int begin = offset;
        int end = offset + len;
        while (begin != end) {
            if (files[ind].getFilePointer() == files[ind].length()) {
                ++ind;
                if (ind == files.length) {
                    throw new EOFException();
                }
                files[ind].seek(0);
            }
            int read = files[ind].read(b, begin, end - begin);
            begin += read;
        }
        return begin - offset;
    }

    public synchronized int read(long pos, byte[] b, int offset, int len) throws IOException {
        seek(pos);
        return read(b, offset, len);
    }

    public int readInt() throws IOException {
        return files[ind].readInt();
    }

    public synchronized int readInt(long pos) throws IOException {
        seek(pos);
        return readInt();
    }

    public long readLong() throws IOException {
        return files[ind].readLong();
    }

    public synchronized long readLong(long pos) throws IOException {
        seek(pos);
        return files[ind].readLong();
    }

    public void writeByte(byte b) throws IOException {
        files[ind].writeByte(b);
    }

    public synchronized void writeByte(long pos, byte b) throws IOException {
        seek(pos);
        files[ind].writeByte(b);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public synchronized void write(long pos, byte[] b) throws IOException {
        write(pos, b, 0, b.length);
    }

    public void write(byte[] b, int offset, int len) throws IOException {
        int begin = offset;
        int end = begin + len;
        while (begin != end) {
            if (files[ind].getFilePointer() == files[ind].length()) {
                ++ind;
                if (ind == files.length) {
                    throw new EOFException();
                }
                files[ind].seek(0);
            }
            int toWrite = (int)Math.min(end - begin, files[ind].length() - files[ind].getFilePointer());
            files[ind].write(b, begin, toWrite);
            begin += toWrite;
        }
    }

    public synchronized void write(long pos, byte[] b, int offset, int len) throws IOException {
        seek(pos);
        write(b, offset, len);
    }

    public void writeInt(int x) throws IOException {
        files[ind].writeInt(x);
    }

    public synchronized void writeInt(long pos, int x) throws IOException {
        seek(pos);
        files[ind].writeInt(x);
    }

    public void writeLong(long x) throws IOException {
        files[ind].writeLong(x);
    }

    public synchronized void writeLong(long pos, long x) throws IOException {
        seek(pos);
        files[ind].writeLong(x);
    }

    public synchronized void writeDnaQ(long pos, DnaQ d) throws IOException {
        int oldLen = readInt(pos);
        int newLen = d.length();
        seek(pos);
        writeInt(newLen);
        write(d.toByteArray());
        for (int i = newLen; i < oldLen; ++i) {
            writeByte((byte)-1);
        }
    }

}
