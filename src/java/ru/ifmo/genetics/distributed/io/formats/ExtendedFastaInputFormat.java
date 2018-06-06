package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.io.readers.ExtendedFastaRecordReader;
import ru.ifmo.genetics.io.readers.FastaRecordReader;

import java.io.IOException;

public class ExtendedFastaInputFormat extends FileInputFormat<Text, DnaQWritable> {
    @Override
    public RecordReader<Text, DnaQWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new ExtendedFastaRecordReader(job, (FileSplit) split);
    }
}
