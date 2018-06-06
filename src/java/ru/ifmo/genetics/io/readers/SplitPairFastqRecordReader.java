package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.IOException;

public class SplitPairFastqRecordReader implements RecordReader<Text, PairedDnaQWritable> {
    private final FastqRecordReader reader;
    boolean secondInPair;

    public SplitPairFastqRecordReader(JobConf job, FileSplit split) throws IOException {
        char c = TextUtils.getLastDigit(split.getPath().toString());
        switch (c) {
            case '1': secondInPair = false; break;
            case '2': secondInPair = true; break;
            default: throw new IllegalArgumentException("Last digit of split fastq files should be 1 or 2");
        }
        reader = new FastqRecordReader(job, split);
    }


    @Override
    public boolean next(Text text, PairedDnaQWritable pairedDnaQWritable) throws IOException {
        if (!secondInPair) {
            pairedDnaQWritable.second.clear();
            return reader.next(text, pairedDnaQWritable.first);
        } else {
            pairedDnaQWritable.first.clear();
            return reader.next(text, pairedDnaQWritable.second);
        }
    }

    @Override
    public Text createKey() {
        return new Text();
    }

    @Override
    public PairedDnaQWritable createValue() {
        return new PairedDnaQWritable();
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
