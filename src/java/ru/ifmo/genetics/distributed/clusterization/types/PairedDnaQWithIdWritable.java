package ru.ifmo.genetics.distributed.clusterization.types;

import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;

/**
* Author: Sergey Melnikov
*/
public class PairedDnaQWithIdWritable extends PairWritable<Int128WritableComparable, PairedDnaQWritable> {
    public PairedDnaQWithIdWritable() {
        this(new Int128WritableComparable(), new PairedDnaQWritable());
    }

    public PairedDnaQWithIdWritable(Int128WritableComparable first, PairedDnaQWritable second) {
        super(first, second);
    }

    public void setFieldsFrom(PairedDnaQWithIdWritable other) {
        first.copyFieldsFrom(other.first);
        second.setFieldsFrom(other.second);
    }

}
