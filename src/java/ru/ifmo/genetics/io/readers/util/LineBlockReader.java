package ru.ifmo.genetics.io.readers.util;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.LineReader;

import java.io.IOException;
import java.io.InputStream;

public class LineBlockReader {
    InputStream inputStream;
    LineReader lr;
    long position;
    long start;
    long end;


    public interface BlockMatcher {
        public boolean isBlockGood(Text[] block);
    }

    Text[] block;
    int BLOCK_SIZE;
    BlockMatcher blockMatcher;

    boolean blockInited = false;

    public LineBlockReader(InputStream in, Text[] block, BlockMatcher blockMatcher) throws IOException {
        this(in, 0, Long.MAX_VALUE, block, blockMatcher);
        
    }

    protected LineBlockReader(InputStream in, long start, long end, Text[] block, BlockMatcher blockMatcher) throws IOException {
        assert start == 0 || in instanceof Seekable;
        this.position = start;
        this.start = start;
        this.end = end;
        this.inputStream = in;
        
        if (start != 0) {
            ((Seekable)in).seek(position);
        }
        
        lr = new LineReader(in);

        this.blockMatcher = blockMatcher;
        this.block = block;
        BLOCK_SIZE = block.length;
    }

    public LineBlockReader(JobConf job, FileSplit split, Text[] block, BlockMatcher blockMatcher) throws IOException {
        this(FileSystem.get(job).open(split.getPath()), split.getStart(), split.getStart() + split.getLength(), block, blockMatcher);
    }

    private boolean initBlock() throws IOException {
        blockInited = true;
        if (start == 0) {
            return readBlock();
        }

        // skip bytes to newline
        int read = lr.readLine(block[0]);
        if (read == 0) {
            return false;
        } else {
            position += read;
        }

        if (!readBlock()) {
            return false;

        }

        int tryNumber = 0;
        while (!blockMatcher.isBlockGood(block)) {
            tryNumber++;
            // in one of BLOCK_SIZE consecutive lines block should start
            assert tryNumber < BLOCK_SIZE;
            Text t = block[0];
            System.arraycopy(block, 1, block, 0, BLOCK_SIZE - 1);
            block[BLOCK_SIZE - 1] = t;
            read = lr.readLine(block[BLOCK_SIZE - 1]);
            if (read == 0) {
                return false;
            }
            position += read;
        }
        return true;
    }

    public boolean readBlock() throws IOException {
        if (!blockInited) {
            return initBlock();
        }

        if (position > end) {
            return false;
        }
        for (int i = 0; i < BLOCK_SIZE; ++i) {
            int read = lr.readLine(block[i]);
            if (read == 0) {
                return false;
            }
            position += read;
        }
        return true;
    }


    public long getPos() throws IOException {
        return position;
    }

    public void close() throws IOException {
        lr.close();
    }

    public float getProgress() {
        return ProgressMeter.getProgress(start, position, end);
    }

}
