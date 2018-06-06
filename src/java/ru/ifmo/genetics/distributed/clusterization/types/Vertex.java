package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

/**
* Author: Sergey Melnikov
*/
public class Vertex extends Int128WritableComparable {
    public Vertex() {
    }

    public Vertex(Int128WritableComparable value) {
        super(value);
    }
}
