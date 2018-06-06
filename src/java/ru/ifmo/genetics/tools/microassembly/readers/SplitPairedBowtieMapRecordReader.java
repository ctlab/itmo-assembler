package ru.ifmo.genetics.tools.microassembly.readers;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.tools.microassembly.types.PairedBowtieAlignmentWritable;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.IOException;

public class SplitPairedBowtieMapRecordReader implements RecordReader<LongWritable, PairedBowtieAlignmentWritable> {
    BowtieMapRecordReader reader;
    boolean secondInPair;

    public SplitPairedBowtieMapRecordReader(JobConf job, FileSplit split) throws IOException {
        char c = TextUtils.getLastDigit(split.getPath().toString());
        switch (c) {
            case '1': secondInPair = false; break;
            case '2': secondInPair = true; break;
            default: throw new IllegalArgumentException("Last digit of split fastq files should be 1 or 2");
        }
        reader = new BowtieMapRecordReader(job, split);
    }

    @Override
    public boolean next(LongWritable longWritable, PairedBowtieAlignmentWritable pairedBowtieAlignmentWritable) throws IOException {
        pairedBowtieAlignmentWritable.setNotNullness(!secondInPair, secondInPair);
        if (!secondInPair) {
            return reader.next(longWritable, pairedBowtieAlignmentWritable.first());
        } else {
            return reader.next(longWritable, pairedBowtieAlignmentWritable.second());
        }
    }

    @Override
    public LongWritable createKey() {
        return new LongWritable();
    }

    @Override
    public PairedBowtieAlignmentWritable createValue() {
        return new PairedBowtieAlignmentWritable();
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
