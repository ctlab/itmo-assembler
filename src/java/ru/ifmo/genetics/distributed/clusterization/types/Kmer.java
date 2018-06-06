package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.LongWritable;

/**
 * Author: Sergey Melnikov
 */
public class Kmer extends LongWritable {
    public Kmer() {
    }

    public Kmer(long value) {
        super(value);
    }

    public Kmer(Kmer second) {
        super(second.get());
    }
}
