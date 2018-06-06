package ru.ifmo.genetics.distributed.io;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SequencedWritableRecordReader<V extends Writable> {
    private FSDataInputStream realIn;
    private MyInputStream internalIn;
    private DataInputStream in;
    private boolean borderCrossed = true;
    private final static int BLOCK_SIZE = SequencedWritableOutput.BLOCK_SIZE;
    private final static int BLOCK_SIZE_MASK = SequencedWritableOutput.BLOCK_SIZE_MASK;

    public SequencedWritableRecordReader(JobConf job, Path file, long start) throws IOException {

        // open the file and seek to the start of the split
        final FileSystem fs = file.getFileSystem(job);
        realIn = fs.open(file);
        realIn.seek(start);

        internalIn = new MyInputStream();
        in = new DataInputStream(internalIn);
    }
    
    public long position() throws IOException {
        return realIn.getPos();
        
    }

    public boolean canFinishReading() {
        return borderCrossed;

    }
    
    public void readRecord(V value) throws IOException {
//        System.err.println("before reading record: " + position());
        borderCrossed = false;
        long prevPosition = position();
        try {
            value.readFields(in);
        } catch (IOException e) {
            System.err.println("position before fail: " + prevPosition);
            throw e;
        } catch (RuntimeException e) {
            System.err.println("position before fail: " + prevPosition);
            throw e;
        }
//        System.err.println("after reading record: " + position());
    }
    
    public void close() throws IOException {
        in.close();
    }

    private class MyInputStream extends InputStream {
        MyInputStream() throws IOException {
//            System.err.println("Starting from " + position);
            long seek_position = (position() + BLOCK_SIZE - 1) & ~(long)BLOCK_SIZE_MASK;
//            System.err.println("Seeking to " + position);
            realIn.seek(seek_position);
            realIn.getPos();
            while (true) {
                int toSkip = tryReadHeader();
                if (toSkip < 0) {
                    break;
                }

//                System.err.println("Skipping " + toSkip + " bytes");
                toSkip -= realIn.skipBytes(toSkip);
                while (toSkip > 0) {
                    realIn.readByte();
                    toSkip--;
                    realIn.skipBytes(toSkip);
                }
//                System.err.println("position after skiping: " + position);
            }
            borderCrossed = true;

        }
        
        private int tryReadHeader() throws IOException {
            if ((position() & BLOCK_SIZE_MASK) == 0) {
                int res = realIn.readInt();
                borderCrossed = true;
                return res;
            }
            return -1;

        }
        
        public int read() throws IOException {
            tryReadHeader();
            return realIn.read();
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return read(bytes, 0, bytes.length);
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            tryReadHeader();
            int toRead = Math.min(len, BLOCK_SIZE - (int)((position() & BLOCK_SIZE_MASK)));
            int read = realIn.read(bytes, off, toRead);
            return read;
        }

        @Override
        public int available() throws IOException {
            return realIn.available();
        }
        
        @Override
        public void close() throws IOException {
            realIn.close();
        }
    }
}
