package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.LongWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

/**
* Author: Sergey Melnikov
*/
public class ComponentID extends Int128WritableComparable {
    public ComponentID() {
    }

    public ComponentID(Int128WritableComparable value) {
        super(value);
    }
}
