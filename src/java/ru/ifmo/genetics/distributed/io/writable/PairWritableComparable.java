package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.WritableComparable;

public class PairWritableComparable<A extends WritableComparable<A>, B extends WritableComparable<B>>
        extends PairWritable<A, B>
        implements WritableComparable<PairWritableComparable<A, B>> {

    public PairWritableComparable(A first, B second) {
        super(first, second);
    }
    
    @Override
    public int compareTo(PairWritableComparable<A, B> o) {
        int res = first.compareTo(o.first);
        if (res != 0)
            return res;

        return second.compareTo(o.second);
    }
}
