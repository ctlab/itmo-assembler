package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.tools.microassembly.readers.SplitPairedBowtieMapRecordReader;
import ru.ifmo.genetics.tools.microassembly.types.PairedBowtieAlignmentWritable;

import java.io.IOException;

public class SplitBowtieMapInputFormat extends FileInputFormat<LongWritable, PairedBowtieAlignmentWritable> {
    @Override
    public RecordReader<LongWritable, PairedBowtieAlignmentWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new SplitPairedBowtieMapRecordReader(job, (FileSplit)split);
    }
}
