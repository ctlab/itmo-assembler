package ru.ifmo.genetics.structures.arrays;

import org.apache.hadoop.io.Writable;

public interface BigArray extends Writable {
    public void reset(long newSize);
}
