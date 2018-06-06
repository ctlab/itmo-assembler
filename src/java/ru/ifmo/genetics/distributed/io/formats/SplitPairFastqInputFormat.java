package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.io.readers.SplitPairFastqRecordReader;

import java.io.IOException;

public class SplitPairFastqInputFormat extends FileInputFormat<Text, PairedDnaQWritable> {

    @Override
    public RecordReader<Text, PairedDnaQWritable> getRecordReader(InputSplit inputSplit, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(inputSplit.toString());
        return new SplitPairFastqRecordReader(job, (FileSplit) inputSplit);
    }
}
