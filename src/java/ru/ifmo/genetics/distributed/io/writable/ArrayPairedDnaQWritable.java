package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.LongWritable;

import java.util.Arrays;

public class ArrayPairedDnaQWritable extends ArrayWritable {
    public ArrayPairedDnaQWritable() {
        super(PairedDnaQWritable.class);
    }

    @Override
    public String toString() {
        return Arrays.toString(toStrings());
    }
}
