package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.ArrayWritable;
import ru.ifmo.genetics.distributed.io.writable.*;

/**
 * Author: Sergey Melnikov
 */
public class Int128ArrayWritable extends ArrayWritable{
    public Int128ArrayWritable() {
        super(Int128WritableComparable.class);
    }
}
