package ru.ifmo.genetics.io.readers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.io.formats.Sanger;
import ru.ifmo.genetics.io.readers.util.LineBlockReader;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.IOException;
import java.io.InputStream;

public class ExtendedFastaRecordReader implements RecordReader<Text, DnaQWritable> {
    LineBlockReader reader;
    Log log = LogFactory.getLog(ExtendedFastaRecordReader.class);
    final static int BLOCK_SIZE = 2;

    Text[] block = new Text[BLOCK_SIZE];
    {
        for (int i = 0; i < BLOCK_SIZE; ++i) {
            block[i] = new Text();
        }
    }

    private static class FastaBlockMatcher implements LineBlockReader.BlockMatcher {
        @Override
        public boolean isBlockGood(Text[] block) {
            assert block.length == BLOCK_SIZE;
            return TextUtils.startsWith(block[0], ">");
        }
    }

    private final FastaBlockMatcher fastaBlockMatcher = new FastaBlockMatcher();

    public ExtendedFastaRecordReader(InputStream in) throws IOException {
        reader = new LineBlockReader(in, block, fastaBlockMatcher);
    }

    public ExtendedFastaRecordReader(JobConf job, FileSplit split) throws IOException {
        reader = new LineBlockReader(job, split, block, fastaBlockMatcher);
    }

    @Override
    public boolean next(Text key, DnaQWritable value) throws IOException {
        if (!reader.readBlock()) {
            return false;
        }
        key.set(block[0].getBytes(), 1, block[0].getLength() - 1);
        value.set(block[1], new Text(TextUtils.multiply((char)(33 + 20), block[1].getLength())), Sanger.instance);
        return true;
    }

    @Override
    public Text createKey() {
        return new Text();
    }

    @Override
    public DnaQWritable createValue() {
        return new DnaQWritable();
    }

    @Override
    public long getPos() throws IOException {
        return reader.getPos();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public float getProgress() throws IOException {
        return reader.getProgress();
    }
}
