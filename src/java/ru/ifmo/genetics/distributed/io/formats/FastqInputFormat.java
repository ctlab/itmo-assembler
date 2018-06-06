package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.io.readers.FastqRecordReader;

import java.io.IOException;

public class FastqInputFormat extends FileInputFormat<Text, DnaQWritable> {

    @Override
    public RecordReader<Text, DnaQWritable> getRecordReader(InputSplit inputSplit, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(inputSplit.toString());
        return new FastqRecordReader(job, (FileSplit) inputSplit);
    }
}
