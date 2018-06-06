package ru.ifmo.genetics.io.readers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ReaderInSmallMemory {
    private ByteBuffer fileBuffer;
    private FileChannel channel;
    private StringBuilder line;

    public ReaderInSmallMemory(File file) throws IOException {
        channel = new FileInputStream(file).getChannel();
        fileBuffer = ByteBuffer.allocate(1 << 12);
        fileBuffer.position(fileBuffer.capacity());
        line = new StringBuilder();
    }

    public boolean hasRemaining() throws IOException {
        while (!fileBuffer.hasRemaining()) {
            fileBuffer.clear();
            int readBytes = channel.read(fileBuffer);
            fileBuffer.flip();
            // System.err.println(readBytes);
            if (readBytes == -1) {
                return false;
            }
        }
        return true;

    }

    public int readInteger() throws IOException {
        int res = 0;
        boolean start = true;
        int sign = 1;
        while (hasRemaining()) {
            byte c = fileBuffer.get();
            if (c == '\r' || c == '\n' || c == ' ') {
                if (start) {
                    continue;
                } else {
                    break;
                }
            }
            start = false;
            if (c == '-') {
                sign = -1;
                continue;
            }
            res = res * 10 + (c - '0');
        }
        if (start) {
            return -1;
        }
        res *= sign;
        return res;
    }

    public CharSequence readLine() throws IOException {
        boolean start = true;
        line.delete(0, line.length());
        while (hasRemaining()) {
            byte c = fileBuffer.get();
            if (c == '\r' || c == '\n') {
                if (start) {
                    continue;
                } else {
                    break;
                }
            }

            start = false;
            line.append((char)c);
        }
        if (start) {
            return null;
        }
        return line;
    }

    public void close() throws IOException {
        channel.close();
    }
}
