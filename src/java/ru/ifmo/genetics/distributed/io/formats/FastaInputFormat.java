package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.io.readers.FastaRecordReader;

import java.io.IOException;

public class FastaInputFormat extends FileInputFormat<Text, DnaWritable> {
    @Override
    public RecordReader<Text, DnaWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new FastaRecordReader(job, (FileSplit) split);
    }
}
