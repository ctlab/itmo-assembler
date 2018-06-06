package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.tools.microassembly.readers.BowtieMapRecordReader;
import ru.ifmo.genetics.tools.microassembly.types.BowtieAlignmentWritable;

import java.io.IOException;

public class BowtieMapInputFormat extends FileInputFormat<LongWritable, BowtieAlignmentWritable> {
    @Override
    public RecordReader<LongWritable, BowtieAlignmentWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new BowtieMapRecordReader(job, (FileSplit)split);
    }
}
