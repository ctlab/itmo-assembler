package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.io.readers.util.LineBlockReader;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.*;

public class FastqRecordReader implements RecordReader<Text, DnaQWritable> {
    public static String QUALITY_FORMAT = "qualityFormat";
    QualityFormat qf;
    LineBlockReader reader;

    private static final int BLOCK_SIZE = 4;

    Text[] block = new Text[BLOCK_SIZE];
    {
        for (int i = 0; i < BLOCK_SIZE; ++i) {
            block[i] = new Text();
        }
    }

    private static class FastqBlockMatcher implements LineBlockReader.BlockMatcher{

        @Override
        public boolean isBlockGood(Text[] block) {
            assert block.length == BLOCK_SIZE;
            return (TextUtils.startsWith(block[0], "@") && TextUtils.startsWith(block[2], "+"));
        }
    }

    private FastqBlockMatcher fastqBlockMatcher = new FastqBlockMatcher();

    public FastqRecordReader(InputStream in, QualityFormat qf) throws IOException {
        reader = new LineBlockReader(in, block, fastqBlockMatcher);
        this.qf = qf;
    }

    public FastqRecordReader(JobConf job, FileSplit split) throws IOException {
        qf = QualityFormatFactory.instance.get(job.get(QUALITY_FORMAT));
        reader = new LineBlockReader(job, split, block, fastqBlockMatcher);
    }


    @Override
    public boolean next(Text text, DnaQWritable dnaQWritable) throws IOException {
        if (!reader.readBlock())
            return false;
        
        text.set(block[0].getBytes(), 1, block[0].getLength() - 1);
        dnaQWritable.set(block[1], block[3], qf);
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
