package ru.ifmo.genetics.distributed.io;

import org.apache.hadoop.io.Writable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SequencedWritableOutput<V extends Writable> {
    public final static int BLOCK_SIZE = 32768;
    public final static int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;
    public final static int HEADER_LENGTH = Integer.SIZE / 8;
    private FileChannel realOut;
    private MyOutputStream internalOut;
    private DataOutput out;

    public SequencedWritableOutput(String filename) throws FileNotFoundException {
        realOut = new FileOutputStream(filename).getChannel();
        internalOut = new MyOutputStream();
        out = new DataOutputStream(internalOut);
    }

    public void append(V value) throws IOException {
        value.write(out);
        internalOut.recordFinished();
    }
    
    public void close() throws IOException {
        internalOut.close();
    }

    private class MyOutputStream extends OutputStream {
        private ByteBuffer buf = ByteBuffer.allocate(BLOCK_SIZE);
        private int firsRecordOffset = -1;

        public MyOutputStream() {
            super();    //To change body of overridden methods use File | Settings | File Templates.
            buf.position(HEADER_LENGTH);
            recordFinished();
        }

        @Override
        public void write(int i) throws IOException {
            if (!buf.hasRemaining()) {
                interblockFlush();                
            }
            buf.put((byte)i);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            do {
                int toWrite = Math.min(len, buf.remaining());
                buf.put(bytes, off, toWrite);
                len -= toWrite;
                off += toWrite;
                if (!buf.hasRemaining()) {
                    interblockFlush();
                }
            } while (len > 0);
        }

        @Override
        public void flush() throws IOException {
            throw new UnsupportedOperationException("flush in MyOutputStream is not supported");
        }

        private void interblockFlush() throws IOException {
            if (firsRecordOffset == -1) {
                firsRecordOffset = buf.position() - HEADER_LENGTH;
            }
            buf.putInt(0, firsRecordOffset);
            buf.flip();
            while (buf.hasRemaining()) {
                realOut.write(buf);
            }
            
            buf.clear();
            buf.position(HEADER_LENGTH);
            firsRecordOffset = -1;
        }

        @Override
        public void close() throws IOException {
            interblockFlush();
            realOut.close();
        }

        public void recordFinished() {
            if (firsRecordOffset == -1) {
                firsRecordOffset = buf.position() - HEADER_LENGTH;
            }
        }
    }
}
