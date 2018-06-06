package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.LongWritable;

public class LongArrayWritable extends ArrayWritable {
    public LongArrayWritable() {
        super(LongWritable.class);
    }
}
